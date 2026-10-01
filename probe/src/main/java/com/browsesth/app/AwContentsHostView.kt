package com.browsesth.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import org.chromium.android_webview.AwBrowserContext
import org.chromium.android_webview.AwContents
import org.chromium.android_webview.AwContentsClient
import org.chromium.android_webview.AwSettings
import org.chromium.android_webview.gfx.AwDrawFnImpl
import org.chromium.base.Callback

/**
 * Hosts Aloha [AwContents] in place of android.webkit.WebView.
 * Software-layer path adapted from Chromium AwTestContainerView.
 */
class AwContentsHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private var awContents: AwContents? = null
    private val internalAccessDelegate = InternalAccessAdapter()
    private val drawFnAccess = AwDrawFnImpl.DrawFnAccess { _, _ ->
        // Software path: functor draw is unused.
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        overScrollMode = OVER_SCROLL_ALWAYS
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun attach(client: AwContentsClient): AwContents {
        destroyContents()
        val browserContext = AwBrowserContext.getDefault()
            ?: error("AwBrowserContext.getDefault() is null — browser process not ready")
        val settings = AwSettings(
            context,
            /* isAccessFromFileUrlsGrantedByDefault= */ false,
            /* supportsLegacyQuirks= */ false,
            /* allowEmptyDocumentPersistence= */ false,
            /* allowGeolocationOnInsecureOrigins= */ true,
            /* doNotUpdateSelectionOnMutatingSelectionRange= */ false,
        )
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(true)
        settings.setBuiltInZoomControls(true)
        settings.setDisplayZoomControls(false)
        settings.setUseWideViewPort(true)
        settings.setLoadWithOverviewMode(true)

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
        Log.i(TAG, "AwContents attached")
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
    }
}
