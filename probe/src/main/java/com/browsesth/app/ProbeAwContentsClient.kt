package com.browsesth.app

import android.graphics.Bitmap
import android.graphics.Picture
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.View
import com.alohamobile.bromium.BromiumClient
import org.chromium.android_webview.AwConsoleMessage
import org.chromium.android_webview.AwContentsClient
import org.chromium.android_webview.AwContentsClientBridge
import org.chromium.android_webview.AwGeolocationPermissions
import org.chromium.android_webview.AwHttpAuthHandler
import org.chromium.android_webview.AwRenderProcess
import org.chromium.android_webview.AwRenderProcessGoneDetail
import org.chromium.android_webview.AwWebResourceError
import org.chromium.android_webview.AwWebResourceRequest
import org.chromium.android_webview.JsPromptResultReceiver
import org.chromium.android_webview.JsResultReceiver
import org.chromium.android_webview.SafeBrowsingAction
import org.chromium.android_webview.permission.AwPermissionRequest
import org.chromium.android_webview.safe_browsing.AwSafeBrowsingResponse
import org.chromium.base.Callback
import org.chromium.components.embedder_support.util.WebResourceResponseInfo
import java.io.ByteArrayInputStream
import java.security.Principal
import java.util.concurrent.atomic.AtomicInteger

/**
 * Minimal AwContentsClient for Slice R runtime lock probes.
 * Intercepts URLs containing [AD_PROBE_MARKER]; overrides nav URLs containing [MIDPAGE_MARKER].
 */
class ProbeAwContentsClient(
    private val onStatus: (String) -> Unit = {},
    private val onNavigationOverride: ((String) -> Unit)? = null,
    private val onPageFinishedExtra: ((String, String) -> Unit)? = null,
) : AwContentsClient() {

    val interceptHits = AtomicInteger(0)
    val navOverrideHits = AtomicInteger(0)
    val pageFinishedCount = AtomicInteger(0)
    @Volatile var lastTitle: String = ""
    @Volatile var lastUrl: String = ""
    @Volatile var lastError: String? = null

    override fun hasWebViewClient(): Boolean = true

    override fun shouldInterceptRequest(request: AwWebResourceRequest): WebResourceResponseInfo? {
        val url = request.url ?: return null
        if (url.contains(AD_PROBE_MARKER)) {
            val n = interceptHits.incrementAndGet()
            Log.i(TAG, "intercept #$n $url")
            onStatus("拦截命中 #$n")
            return WebResourceResponseInfo(
                "text/plain",
                "utf-8",
                ByteArrayInputStream(ByteArray(0)),
                404,
                "Blocked",
                emptyMap(),
            )
        }
        return null
    }

    override fun shouldOverrideUrlLoading(request: AwWebResourceRequest): Boolean {
        val url = request.url ?: return false
        if (url.contains(MIDPAGE_MARKER)) {
            val n = navOverrideHits.incrementAndGet()
            Log.i(TAG, "nav-override #$n $url")
            onStatus("跳转干预 #$n")
            val target = extractSkipTarget(url) ?: "https://example.com/"
            onNavigationOverride?.invoke(target)
            return true
        }
        return false
    }

    override fun onPageStarted(url: String) {
        lastUrl = url
        lastError = null
        onStatus("加载中: $url")
    }

    override fun onPageFinished(url: String) {
        lastUrl = url
        pageFinishedCount.incrementAndGet()
        onStatus("加载完成: $url")
        onPageFinishedExtra?.invoke(url, lastTitle)
    }

    override fun onReceivedTitle(title: String) {
        lastTitle = title
    }

    override fun onReceivedError(request: AwWebResourceRequest, error: AwWebResourceError) {
        if (request.isOutermostMainFrame) {
            lastError = "${error.description} (net=${error.netError})"
            onStatus("错误: $lastError")
        }
    }

    override fun getVisitedHistory(callback: Callback<Array<String>>) {}
    override fun doUpdateVisitedHistory(url: String, isReload: Boolean) {}
    override fun onProgressChanged(progress: Int) {}
    override fun shouldOverrideKeyEvent(event: KeyEvent): Boolean = false
    override fun onLoadResource(url: String) {}
    override fun onUnhandledKeyEvent(event: KeyEvent) {}
    override fun onConsoleMessage(consoleMessage: AwConsoleMessage): Boolean = false

    override fun onReceivedHttpAuthRequest(handler: AwHttpAuthHandler, host: String, realm: String) {
        handler.cancel()
    }

    override fun onReceivedSslError(callback: Callback<Boolean>, error: SslError) {
        callback.onResult(false)
    }

    override fun onReceivedClientCertRequest(
        callback: AwContentsClientBridge.ClientCertificateRequestCallback,
        keyTypes: Array<String>,
        principals: Array<Principal>,
        host: String,
        port: Int,
    ) {
        callback.proceed(null, null)
    }

    override fun onReceivedLoginRequest(realm: String, account: String, args: String) {}
    override fun onFormResubmission(dontResend: Message, resend: Message) {
        dontResend.sendToTarget()
    }

    override fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
    ) {}

    override fun showFileChooser(
        uploadFilePathsCallback: Callback<Array<String>>,
        fileChooserParams: FileChooserParamsImpl,
    ) {}

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: AwGeolocationPermissions.Callback,
    ) {}

    override fun onGeolocationPermissionsHidePrompt() {}
    override fun onPermissionRequest(awPermissionRequest: AwPermissionRequest) {
        awPermissionRequest.deny()
    }

    override fun onPermissionRequestCanceled(awPermissionRequest: AwPermissionRequest) {}
    override fun onScaleChangedScaled(oldScale: Float, newScale: Float) {}
    override fun onReceivedTouchIconUrl(url: String, precomposed: Boolean) {}
    override fun onReceivedIcon(bitmap: Bitmap) {}
    override fun onPageCommitVisible(url: String) {}
    override fun onReceivedHttpError(request: AwWebResourceRequest, response: WebResourceResponseInfo) {}
    override fun onShowCustomView(view: View, callback: CustomViewCallback) {}
    override fun onHideCustomView() {}
    override fun getDefaultVideoPoster(): Bitmap? = null
    override fun onFindResultReceived(activeMatchOrdinal: Int, numberOfMatches: Int, isDoneCounting: Boolean) {}
    override fun onNewPicture(picture: Picture) {}
    override fun onRendererUnresponsive(process: AwRenderProcess) {}
    override fun onRendererResponsive(process: AwRenderProcess) {}
    override fun onRenderProcessGone(detail: AwRenderProcessGoneDetail): Boolean = false

    override fun handleJsAlert(url: String, message: String, receiver: JsResultReceiver) {
        receiver.cancel()
    }

    override fun handleJsBeforeUnload(url: String, message: String, receiver: JsResultReceiver) {
        receiver.confirm()
    }

    override fun handleJsConfirm(url: String, message: String, receiver: JsResultReceiver) {
        receiver.cancel()
    }

    override fun handleJsPrompt(
        url: String,
        message: String,
        defaultValue: String,
        receiver: JsPromptResultReceiver,
    ) {
        receiver.cancel()
    }

    override fun onCreateWindow(
        isDialog: Boolean,
        isUserGesture: Boolean,
        targetUrl: String,
        isGoogleAuth3PCookiesRequired: Boolean,
    ): Boolean = false

    override fun onCloseWindow() {}
    override fun onRequestFocus() {}
    override fun getVideoLoadingProgressView(): View? = null

    override fun onSafeBrowsingHit(
        request: AwWebResourceRequest,
        threatType: Int,
        callback: Callback<AwSafeBrowsingResponse>,
    ) {
        callback.onResult(AwSafeBrowsingResponse(SafeBrowsingAction.SHOW_INTERSTITIAL, true))
    }

    // BromiumClient
    override fun onMediaPlay(
        playerId: BromiumClient.MediaPlayerId,
        mediaUrl: String,
        documentUrl: String,
        durationSec: Double,
        isAudioOnly: Boolean,
    ) {}

    override fun onMediaPause(
        playerId: BromiumClient.MediaPlayerId,
        mediaUrl: String,
        documentUrl: String,
        durationSec: Double,
        isAudioOnly: Boolean,
    ) {}

    override fun onMediaDestroy(playerId: BromiumClient.MediaPlayerId, mediaUrl: String, documentUrl: String) {}

    override fun onMediaError(
        playerId: BromiumClient.MediaPlayerId,
        pipelineStatus: String,
        mediaUrl: String,
        documentUrl: String,
        currentTimeSec: Double,
        durationSec: Double,
    ) {}

    override fun shouldHideControlsInFullscreen(
        playerId: BromiumClient.MediaPlayerId,
        url: String,
        durationSec: Double,
        playerClass: String,
    ): Boolean = false

    override fun onVideoEnterFullscreen(playerId: BromiumClient.MediaPlayerId, url: String, mediaControlsIsHidden: Boolean) {}
    override fun onVideoExitFullscreen(playerId: BromiumClient.MediaPlayerId, url: String) {}
    override fun onEnterFullscreen() {}
    override fun onExitFullscreen() {}
    override fun onPageLoaded(url: String, isError: Boolean) {}
    override fun onInternalDownloadStarted(httpMethod: String, sizeInBytes: Int) {}

    override fun onDownloadToCacheFinished(
        url: String,
        originalUrl: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        suggestedFilename: String,
        downloadedFilePath: String,
    ) {}

    override fun onRequestedDownloadUrl(url: String) {}
    override fun shouldPlayBackgroundVideo(): Boolean = false
    override fun onHlsDetected(url: String, requestHeaders: String) {}
    override fun getUserAgent(): String =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36 Browsesth/0.4"

    companion object {
        private const val TAG = "BrowsesthProbe"
        const val AD_PROBE_MARKER = "browsesth-ad-probe"
        const val MIDPAGE_MARKER = "browsesth-midpage-skip"

        fun extractSkipTarget(url: String): String? {
            val marker = "to="
            val idx = url.indexOf(marker)
            if (idx < 0) return null
            return url.substring(idx + marker.length)
        }
    }
}
