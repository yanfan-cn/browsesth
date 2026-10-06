package io.legado.app.browser.engine

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
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
 */
class AwContentsHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private var awContents: AwContents? = null
    private val internalAccessDelegate = InternalAccessAdapter()

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
        settings.setSupportZoom(true)
        settings.setBuiltInZoomControls(true)
        settings.setDisplayZoomControls(false)
        // Wide viewport ON, overview OFF: overview mode often squashes mobile chrome.
        settings.setUseWideViewPort(true)
        settings.setLoadWithOverviewMode(false)
        settings.setMediaPlaybackRequiresUserGesture(true)
        // SPEC-018/029: phone/desktop UA + Client Hints aligned to 150.0.7871.46.
        val ua = BrowserUa.resolve(context)
        val uaMobile = BrowserChromePrefs(context).uaModeOrDefault(
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
        if (BrowserKernelCompat.serpCompatEnabled(context)) {
            runCatching { settings.setAcceptThirdPartyCookies(true) }
                .onFailure { Log.w(TAG, "setAcceptThirdPartyCookies: ${it.message}") }
        }
        // SPEC-013: keep images on for SERP thumbnails / faster perceived load.
        runCatching {
            val m = settings.javaClass.getMethod("setImagesEnabled", Boolean::class.javaPrimitiveType)
            m.invoke(settings, true)
        }
        runCatching {
            val m = settings.javaClass.getMethod(
                "setLoadsImagesAutomatically",
                Boolean::class.javaPrimitiveType,
            )
            m.invoke(settings, true)
        }
        runCatching {
            // WebSettings.LOAD_DEFAULT = -1
            val m = settings.javaClass.getMethod("setCacheMode", Int::class.javaPrimitiveType)
            m.invoke(settings, -1)
        }
        // SPEC-043: Chromium Force Dark / algorithmic darkening (Edge-like).
        applyWebContentDarkMode(settings, BrowserChromePrefs(context).webContentDarkMode)

        val drawFnAccess = BrowserDrawMode.createDrawFnAccess(this)
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
        val mode = if (BrowserDrawMode.preferHardware()) "hardware DrawFn" else "software"
        Log.i(TAG, "AwContents attached (incognito=$incognito, $mode)")
        if (isAttachedToWindow) {
            contents.viewMethods.onAttachedToWindow()
        }
        return contents
    }

    fun contentsOrNull(): AwContents? = awContents

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

    fun canGoBack(): Boolean = awContents?.canGoBack() == true
    fun canGoForward(): Boolean = awContents?.canGoForward() == true
    fun goBack() { awContents?.goBack() }
    fun goForward() { awContents?.goForward() }
    fun reload() { awContents?.reload() }

    fun onHostResume() { awContents?.onResume() }
    fun onHostPause() { awContents?.onPause() }

    /**
     * SPEC-053: downscaled still of the current page for the tab grid.
     * Software canvas only — never used as liquid-glass backdrop.
     */
    fun capturePreview(maxWidthPx: Int = 360): Bitmap? {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return null
        return runCatching {
            val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(full)
            draw(canvas)
            if (w <= maxWidthPx) return@runCatching full
            val nh = (h.toFloat() / w * maxWidthPx).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(full, maxWidthPx, nh, true)
        }.getOrNull()
    }

    fun destroyContents() {
        awContents?.destroy()
        awContents = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        awContents?.viewMethods?.onAttachedToWindow()
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
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Keep vertical/horizontal scrolls inside AwContents; do not let HorizontalPager steal them.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                parent?.requestDisallowInterceptTouchEvent(false)
        }
        val handled = awContents?.viewMethods?.onTouchEvent(event) == true
        return handled || super.onTouchEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = awContents?.viewMethods?.onGenericMotionEvent(event) == true
        return handled || super.onGenericMotionEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        val handled = awContents?.viewMethods?.onHoverEvent(event) == true
        return handled || super.onHoverEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
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
         */
        fun applyWebContentDarkMode(settings: AwSettings, mode: String) {
            val forceMode = when (mode) {
                BrowserChromePrefs.WEB_DARK_OFF -> 0
                BrowserChromePrefs.WEB_DARK_ON -> 2
                else -> 1 // auto
            }
            runCatching {
                settings.javaClass
                    .getMethod("setForceDarkMode", Int::class.javaPrimitiveType)
                    .invoke(settings, forceMode)
            }.onFailure { Log.w(TAG, "setForceDarkMode: ${it.message}") }
            if (forceMode != 0) {
                runCatching {
                    settings.javaClass
                        .getMethod("setForceDarkBehavior", Int::class.javaPrimitiveType)
                        .invoke(settings, 2)
                }.onFailure { Log.w(TAG, "setForceDarkBehavior: ${it.message}") }
                runCatching {
                    settings.javaClass
                        .getMethod(
                            "setAlgorithmicDarkeningAllowed",
                            Boolean::class.javaPrimitiveType,
                        )
                        .invoke(settings, true)
                }.onFailure { Log.w(TAG, "setAlgorithmicDarkeningAllowed: ${it.message}") }
            }
        }
    }
}
