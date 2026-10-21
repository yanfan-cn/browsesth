package io.legado.app.browser.engine

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.accessibility.AccessibilityNodeProvider
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import org.chromium.android_webview.AwContents
import org.chromium.android_webview.AwContentsClient
import org.chromium.android_webview.AwSettings
import org.chromium.base.Callback
/**
 * Hosts Aloha [AwContents] in place of android.webkit.WebView.
 *
 * SPEC-011/020: prefer hardware DrawFn; failure → SOFTWARE for this process only
 * (no sticky prefs; SPEC-009 blank-page rollback path).
 * SPEC-073: forward window insets; align touch/draw scroll path with Chromium host.
 */
class AwContentsHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private var awContents: AwContents? = null
    private var awSettings: AwSettings? = null
    private val internalAccessDelegate = InternalAccessAdapter()

    // SPEC-123 diag: last logged sizes to throttle logs.
    private var diagLastMeasuredW = -1
    private var diagLastMeasuredH = -1
    private var diagLastCanvasW = -1
    private var diagLastCanvasH = -1
    private var diagLastInsets = ""

    init {
        applyLayerForDrawMode()
        overScrollMode = OVER_SCROLL_ALWAYS
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private fun applyLayerForDrawMode() {
        if (BrowserDrawMode.preferHardware()) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        } else {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }

    fun attach(client: AwContentsClient, incognito: Boolean = false): AwContents {
        // SPEC-124 diag: stage timing inside composition-frame attach.
        val t0 = android.os.SystemClock.uptimeMillis()
        destroyContents()
        applyLayerForDrawMode()
        BrowserNtpPrefs.activateCookieJar(incognito)
        val browserContext = BrowserNtpPrefs.browserContext(incognito)
        val settings = AwSettings(
            context,
            /* isAccessFromFileUrlsGrantedByDefault= */ false,
            /* supportsLegacyQuirks= */ false,
            /* allowEmptyDocumentPersistence= */ false,
            /* allowGeolocationOnInsecureOrigins= */ true,
            /* doNotUpdateSelectionOnMutatingSelectionRange= */ false,
        )
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = !incognito
        // Wide viewport ON, overview OFF: overview mode often squashes mobile chrome.
        settings.setUseWideViewPort(true)
        settings.setLoadWithOverviewMode(false)
        settings.setMediaPlaybackRequiresUserGesture(true)
        // SPEC-093: disable built-in Aw hyperlink menu; we show Compose sheet instead.
        runCatching {
            settings.setHyperlinkContextMenuItems(0)
        }.onFailure { Log.w(TAG, "setHyperlinkContextMenuItems: ${it.message}") }
        // SPEC-018/029: phone/desktop UA + Client Hints aligned to 150.0.7871.46.
        val ua = BrowserUa.resolve(context)
        val chromePrefs = BrowserChromePrefs(context)
        val uaMobile = chromePrefs.uaModeOrDefault(
            BrowserFormFactor.isTablet(context),
        ) == BrowserUa.Mode.Mobile
        runCatching {
            settings.setUserAgentString(ua)
        }
        runCatching {
            settings.setUserAgentIsMobile(uaMobile)
        }
        runCatching {
            settings.setUserAgentMetadataFromMap(BrowserUa.clientHintMap(uaMobile))
        }.onFailure { Log.w(TAG, "UA metadata: ${it.message}") }
        // SPEC-120: privacy + force zoom from prefs.
        BrowserPrivacyControls.applyCookieManagers(context)
        BrowserPrivacyControls.applyToSettings(context, settings)
        BrowserPrivacyControls.applyForceZoom(context, settings)
        // SPEC-013: keep images on for SERP thumbnails / faster perceived load.
        // SPEC-153: direct calls — by-name reflection broke under R8 (methods
        // renamed/removed in release, NoSuchMethodException swallowed silently).
        runCatching { settings.setImagesEnabled(true) }
            .onFailure { Log.w(TAG, "setImagesEnabled: ${it.message}") }
        runCatching { settings.setLoadsImagesAutomatically(true) }
            .onFailure { Log.w(TAG, "setLoadsImagesAutomatically: ${it.message}") }
        runCatching {
            // WebSettings.LOAD_DEFAULT = -1
            settings.cacheMode = -1
        }.onFailure { Log.w(TAG, "setCacheMode: ${it.message}") }
        // SPEC-043: Chromium Force Dark / algorithmic darkening (Edge-like).
        applyWebContentDarkMode(settings, chromePrefs.webContentDarkMode)
        // SPEC-107: Chrome/Edge-style accessibility text zoom for webpage fonts only.
        runCatching {
            settings.textZoom = chromePrefs.textZoomPercent
        }.onFailure { Log.w(TAG, "setTextZoom: ${it.message}") }
        // SPEC-066: allow local file / content loads for saved pages.
        // SPEC-153: direct calls (see setImagesEnabled note).
        runCatching { settings.setAllowFileAccess(true) }
            .onFailure { Log.w(TAG, "setAllowFileAccess: ${it.message}") }
        runCatching { settings.setAllowContentAccess(true) }
            .onFailure { Log.w(TAG, "setAllowContentAccess: ${it.message}") }

        val drawFnAccess = BrowserDrawMode.createDrawFnAccess(this)
        val tA = android.os.SystemClock.uptimeMillis()
        val contents = AwContents(
            browserContext,
            this,
            context,
            internalAccessDelegate,
            drawFnAccess,
            client,
            settings,
        )
        awContents = contents
        awSettings = settings
        val mode = if (BrowserDrawMode.preferHardware()) "hardware DrawFn" else "software"
        Log.i(TAG, "AwContents attached (incognito=$incognito, $mode)")
        Log.i(
            TAG,
            "diag124 attach total=${android.os.SystemClock.uptimeMillis() - t0}ms " +
                "(settings=${tA - t0}ms, ctor+wire=${android.os.SystemClock.uptimeMillis() - tA}ms)",
        )
        // SPEC-123 diag: geometry at attach time.
        Log.i(
            TAG,
            "diag123 attach: densityDpi=${resources.displayMetrics.densityDpi} " +
                "view=${width}x${height} attachedToWindow=$isAttachedToWindow " +
                "parent=${parent?.javaClass?.simpleName}",
        )
        if (isAttachedToWindow) {
            contents.viewMethods.onAttachedToWindow()
            requestApplyInsets()
        }
        return contents
    }

    fun contentsOrNull(): AwContents? = awContents

    /** SPEC-066: turn off Force Dark before loading offline HTML snapshots. */
    fun applyForceDarkOff() {
        val settings = awSettings ?: return
        applyWebContentDarkMode(settings, BrowserChromePrefs.WEB_DARK_OFF)
    }

    /** SPEC-107: apply webpage Force Dark to the live session (settings + toolbox). */
    fun applyWebContentDarkMode(mode: String) {
        val settings = awSettings ?: return
        Companion.applyWebContentDarkMode(settings, mode)
    }

    /** SPEC-107: Chrome/Edge accessibility-equivalent webpage text zoom. */
    fun applyTextZoom(percent: Int) {
        val settings = awSettings ?: return
        val zoom = percent.coerceIn(
            BrowserChromePrefs.TEXT_ZOOM_MIN,
            BrowserChromePrefs.TEXT_ZOOM_MAX,
        )
        runCatching {
            settings.textZoom = zoom
        }.onFailure { Log.w(TAG, "applyTextZoom: ${it.message}") }
    }

    /** SPEC-120: re-apply force-zoom prefs to live session. */
    fun applyForcePageZoom(enabled: Boolean) {
        val settings = awSettings ?: return
        BrowserChromePrefs(context).forcePageZoom = enabled
        BrowserPrivacyControls.applyForceZoom(context, settings)
        if (enabled) {
            evaluateJavaScript(BrowserPrivacyControls.forceZoomViewportScript(true), null)
        }
    }

    /** SPEC-120: re-apply privacy prefs to live session. */
    fun applyPrivacySettings() {
        val settings = awSettings ?: return
        BrowserPrivacyControls.applyCookieManagers(context)
        BrowserPrivacyControls.applyToSettings(context, settings)
    }

    fun loadUrl(url: String) {
        awContents?.loadUrl(url) ?: Log.w(TAG, "loadUrl before attach: $url")
    }

    fun loadDataWithBaseURL(
        baseUrl: String,
        data: String,
        mimeType: String,
        encoding: String,
        historyUrl: String?,
    ) {
        awContents?.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }

    fun evaluateJavaScript(script: String, callback: Callback<String>?) {
        awContents?.evaluateJavaScript(script, callback)
    }

    /** SPEC-064: save current page as MHTML (.mht), Chrome/Edge offline style. */
    fun saveWebArchive(
        basename: String,
        autoname: Boolean,
        callback: Callback<String>?,
    ) {
        awContents?.saveWebArchive(basename, autoname, callback)
            ?: callback?.onResult(null)
    }

    fun canGoBack(): Boolean = awContents?.canGoBack() == true
    fun canGoForward(): Boolean = awContents?.canGoForward() == true

    /**
     * SPEC-127: drop in-page back stack after an explicit NTP/omnibox navigation so
     * system back returns to the browser home feed instead of the previous article.
     */
    fun clearHistory() {
        runCatching { awContents?.clearHistory() }
            .onFailure { Log.w(TAG, "clearHistory: ${it.message}") }
    }

    /** SPEC-073: mark history nav so session can skip layout-affecting injects / restore scroll. */
    fun goBack() {
        BrowserScrollMemory.markHistoryNavigation()
        awContents?.goBack()
    }

    fun goForward() {
        BrowserScrollMemory.markHistoryNavigation()
        awContents?.goForward()
    }

    fun reload() { awContents?.reload() }

    /** SPEC-119: apply UA live, then caller should [reload]. */
    fun applyUserAgent(mode: BrowserUa.Mode) {
        val settings = awSettings ?: return
        val mobile = mode == BrowserUa.Mode.Mobile
        runCatching { settings.setUserAgentString(BrowserUa.stringFor(mode)) }
        runCatching { settings.setUserAgentIsMobile(mobile) }
        runCatching {
            settings.setUserAgentMetadataFromMap(BrowserUa.clientHintMap(mobile))
        }.onFailure { Log.w(TAG, "applyUserAgent metadata: ${it.message}") }
    }

    /**
     * SPEC-121: find-in-page via TreeWalker + mark highlights (window.find unreliable).
     * Callback receives JSON `{n,i}` or legacy digit string.
     */
    fun findInPage(query: String, findNext: Boolean, forward: Boolean, callback: Callback<String>?) {
        evaluateJavaScript(
            BrowserFindInPageScript.build(query, findNext = findNext, forward = forward),
            callback,
        )
    }

    /** SPEC-073: restore document scroll after history navigation. */
    fun restoreScrollY(scrollY: Int) {
        if (scrollY <= 0) return
        evaluateJavaScript(
            "(function(){var y=$scrollY;var go=function(){window.scrollTo(0,y);};" +
                "go();requestAnimationFrame(function(){go();setTimeout(go,50);setTimeout(go,200);});})();",
            null,
        )
    }

    fun onHostResume() { awContents?.onResume() }
    fun onHostPause() { awContents?.onPause() }

    /**
     * SPEC-053: downscaled still of the current page for the tab grid.
     * Software canvas only — never used as liquid-glass backdrop.
     *
     * SPEC-124: sync full-res software raster (draw() on a 1080p ARGB_8888
     * canvas) blocked the main thread 500ms+ at every tab switch/back exit,
     * killing the NavDisplay transition and causing the perceived 1–2s delay.
     * Primary path is now async PixelCopy (render-thread copy, main thread
     * returns immediately); the software fallback is low-res only (1/9 of
     * the pixels, tens of ms) and used solely when PixelCopy is unavailable
     * or fails.
     */
    fun capturePreviewAsync(maxWidthPx: Int = 360, onDone: (Bitmap?) -> Unit) {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) {
            onDone(null)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isAttachedToWindow) {
            val window = resolveHostWindow()
            if (window != null) {
                val requested = runCatching {
                    val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val loc = IntArray(2)
                    getLocationInWindow(loc)
                    val srcRect = Rect(loc[0], loc[1], loc[0] + w, loc[1] + h)
                    PixelCopy.request(
                        window,
                        srcRect,
                        full,
                        PixelCopy.OnPixelCopyFinishedListener { result ->
                            if (result == PixelCopy.SUCCESS) {
                                val scaled = downscaleOrNull(full, w, h, maxWidthPx)
                                if (scaled != null) {
                                    Log.i("Diag124", "thumb pixelcopy ok ${w}x${h} -> ${scaled.width}x${scaled.height}")
                                    onDone(scaled)
                                } else {
                                    full.recycle()
                                    Log.i("Diag124", "thumb pixelcopy scale failed -> lowres fallback")
                                    onDone(capturePreviewLowRes(maxWidthPx))
                                }
                            } else {
                                full.recycle()
                                Log.i("Diag124", "thumb pixelcopy result=$result -> lowres fallback")
                                onDone(capturePreviewLowRes(maxWidthPx))
                            }
                        },
                        Handler(Looper.getMainLooper()),
                    )
                }
                if (requested.isSuccess) {
                    return
                }
                Log.i("Diag124", "thumb pixelcopy request threw: ${requested.exceptionOrNull()}")
            } else {
                Log.i("Diag124", "thumb pixelcopy unavailable (no host window) -> lowres fallback")
            }
        }
        Log.i("Diag124", "thumb sync lowres fallback ${w}x${h} max=$maxWidthPx")
        onDone(capturePreviewLowRes(maxWidthPx))
    }

    /** SPEC-124: unwrap ContextWrapper chain to the hosting Activity's Window. */
    private fun resolveHostWindow(): Window? {
        var ctx: Context? = context
        while (ctx != null) {
            if (ctx is Activity) return ctx.window
            ctx = (ctx as? ContextWrapper)?.baseContext
        }
        return null
    }

    private fun downscaleOrNull(full: Bitmap, w: Int, h: Int, maxWidthPx: Int): Bitmap? = runCatching {
        if (w <= maxWidthPx) full
        else {
            val nh = (h.toFloat() / w * maxWidthPx).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(full, maxWidthPx, nh, true)
        }
    }.getOrNull()

    /** SPEC-124: never full-res sync raster; draw scaled so pixels ≈ 1/9. */
    private fun capturePreviewLowRes(maxWidthPx: Int): Bitmap? {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return null
        return runCatching {
            val bw = maxWidthPx.coerceAtMost(w)
            val bh = (h.toFloat() / w * bw).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.scale(bw.toFloat() / w, bh.toFloat() / h)
            draw(canvas)
            bmp
        }.getOrNull()
    }

    fun destroyContents() {
        awContents?.destroy()
        awContents = null
        awSettings = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        awContents?.viewMethods?.onAttachedToWindow()
        requestApplyInsets()
    }

    override fun onDetachedFromWindow() {
        awContents?.viewMethods?.onDetachedFromWindow()
        super.onDetachedFromWindow()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        awContents?.viewMethods?.onConfigurationChanged(newConfig)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        awContents?.viewMethods?.onFocusChanged(focused, direction, previouslyFocusedRect)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        return awContents?.viewMethods?.onCreateInputConnection(outAttrs)
            ?: super.onCreateInputConnection(outAttrs)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return awContents?.viewMethods?.onKeyUp(keyCode, event) == true || super.onKeyUp(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return awContents?.viewMethods?.dispatchKeyEvent(event) == true || super.dispatchKeyEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        awContents?.viewMethods?.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // SPEC-123 diag
        if (measuredWidth != diagLastMeasuredW || measuredHeight != diagLastMeasuredH) {
            Log.i(
                TAG,
                "diag123 onMeasure: $measuredWidth x $measuredHeight " +
                    "(specW=${View.MeasureSpec.toString(widthMeasureSpec)} " +
                    "specH=${View.MeasureSpec.toString(heightMeasureSpec)})",
            )
            diagLastMeasuredW = measuredWidth
            diagLastMeasuredH = measuredHeight
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // SPEC-123 diag
        Log.i(TAG, "diag123 onSizeChanged: $w x $h (old $oldw x $oldh)")
        awContents?.viewMethods?.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        awContents?.viewMethods?.onContainerViewOverScrolled(scrollX, scrollY, clampedX, clampedY)
            ?: super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        awContents?.viewMethods?.onContainerViewScrollChanged(l, t, oldl, oldt)
    }

    override fun computeScroll() {
        awContents?.viewMethods?.computeScroll() ?: super.computeScroll()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        awContents?.viewMethods?.onVisibilityChanged(changedView, visibility)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        awContents?.viewMethods?.onWindowVisibilityChanged(visibility)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        // SPEC-073: Chromium host must receive system insets (visual viewport / sticky bars).
        // SPEC-123 diag: log insets once per change.
        runCatching {
            val key = insets.toString()
            if (key != diagLastInsets) {
                Log.i(TAG, "diag123 insets: $key")
                diagLastInsets = key
            }
        }
        val aw = awContents
        return if (aw != null) {
            aw.onApplyWindowInsets(insets)
        } else {
            super.onApplyWindowInsets(insets)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Keep vertical/horizontal scrolls inside AwContents; do not let HorizontalPager steal them.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                parent?.requestDisallowInterceptTouchEvent(false)
        }
        // SPEC-073: match AwTestContainerView — super first, then AwContents.
        super.onTouchEvent(event)
        return awContents?.viewMethods?.onTouchEvent(event) == true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        super.onGenericMotionEvent(event)
        return awContents?.viewMethods?.onGenericMotionEvent(event) == true
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        super.onHoverEvent(event)
        return awContents?.viewMethods?.onHoverEvent(event) == true
    }

    override fun onDraw(canvas: Canvas) {
        // SPEC-123 diag: log draw canvas size once per change (software path surface).
        if (canvas.width != diagLastCanvasW || canvas.height != diagLastCanvasH) {
            Log.i(TAG, "diag123 onDraw canvas: ${canvas.width} x ${canvas.height} (view $width x $height)")
            diagLastCanvasW = canvas.width
            diagLastCanvasH = canvas.height
        }
        awContents?.viewMethods?.onDraw(canvas)
        super.onDraw(canvas)
    }

    override fun getAccessibilityNodeProvider(): AccessibilityNodeProvider? {
        return awContents?.viewMethods?.accessibilityNodeProvider
            ?: super.getAccessibilityNodeProvider()
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        return awContents?.viewMethods?.performAccessibilityAction(action, arguments) == true ||
            super.performAccessibilityAction(action, arguments)
    }

    private fun hostScrollTo(scrollX: Int, scrollY: Int) {
        scrollTo(scrollX, scrollY)
    }

    private fun hostOverScrollBy(
        deltaX: Int,
        deltaY: Int,
        scrollX: Int,
        scrollY: Int,
        scrollRangeX: Int,
        scrollRangeY: Int,
        maxOverScrollX: Int,
        maxOverScrollY: Int,
        isTouchEvent: Boolean,
    ) {
        overScrollBy(
            deltaX, deltaY, scrollX, scrollY,
            scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent,
        )
    }

    private inner class InternalAccessAdapter : AwContents.InternalAccessDelegate {
        override fun super_onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
            super@AwContentsHostView.onKeyUp(keyCode, event)

        override fun super_dispatchKeyEvent(event: KeyEvent): Boolean =
            super@AwContentsHostView.dispatchKeyEvent(event)

        override fun super_onGenericMotionEvent(event: MotionEvent): Boolean =
            super@AwContentsHostView.onGenericMotionEvent(event)

        override fun super_onConfigurationChanged(newConfig: Configuration) {
            super@AwContentsHostView.onConfigurationChanged(newConfig)
        }

        override fun super_scrollTo(scrollX: Int, scrollY: Int) {
            hostScrollTo(scrollX, scrollY)
        }

        override fun overScrollBy(
            deltaX: Int,
            deltaY: Int,
            scrollX: Int,
            scrollY: Int,
            scrollRangeX: Int,
            scrollRangeY: Int,
            maxOverScrollX: Int,
            maxOverScrollY: Int,
            isTouchEvent: Boolean,
        ) {
            hostOverScrollBy(
                deltaX, deltaY, scrollX, scrollY,
                scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent,
            )
        }

        override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
            super@AwContentsHostView.onScrollChanged(l, t, oldl, oldt)
        }

        override fun setMeasuredDimension(measuredWidth: Int, measuredHeight: Int) {
            super@AwContentsHostView.setMeasuredDimension(measuredWidth, measuredHeight)
        }

        override fun super_getScrollBarStyle(): Int =
            super@AwContentsHostView.getScrollBarStyle()

        override fun super_startActivityForResult(intent: Intent, requestCode: Int) {}
    }

    companion object {
        private const val TAG = "BrowsesthAwHost"

        /**
         * SPEC-043: map pref → AwSettings Force Dark.
         * Constants match Chromium AwSettings: OFF=0, AUTO=1, ON=2;
         * behavior PREFER_MEDIA_QUERY_OVER_FORCE_DARK=2.
         * SPEC-153: direct calls — by-name reflection broke under R8 (release
         * renamed/stripped the members, NoSuchMethodException was swallowed and
         * the 网页背景 toggle silently did nothing).
         */
        fun applyWebContentDarkMode(settings: AwSettings, mode: String) {
            val forceMode = when (mode) {
                BrowserChromePrefs.WEB_DARK_OFF -> 0
                BrowserChromePrefs.WEB_DARK_ON -> 2
                else -> 1 // auto
            }
            runCatching { settings.setForceDarkMode(forceMode) }
                .onFailure { Log.w(TAG, "setForceDarkMode: ${it.message}") }
            if (forceMode != 0) {
                runCatching { settings.setForceDarkBehavior(2) }
                    .onFailure { Log.w(TAG, "setForceDarkBehavior: ${it.message}") }
                runCatching { settings.setAlgorithmicDarkeningAllowed(true) }
                    .onFailure { Log.w(TAG, "setAlgorithmicDarkeningAllowed: ${it.message}") }
            }
        }
    }
}
