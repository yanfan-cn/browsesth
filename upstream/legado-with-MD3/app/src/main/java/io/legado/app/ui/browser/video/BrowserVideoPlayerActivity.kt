package io.legado.app.ui.browser.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.devbrackets.android.exomedia.ui.widget.VideoView
import io.legado.app.R
import io.legado.app.base.BaseComposeActivity
import io.legado.app.browser.engine.BrowserVideoOffer
import io.legado.app.browser.engine.BrowserVideoOfferBus
import io.legado.app.utils.keepScreenOn

/**
 * SPEC-132/135/136/137: native ExoMedia player host Activity.
 */
class BrowserVideoPlayerActivity : BaseComposeActivity() {

    private var videoView: VideoView? = null
    private var offer: BrowserVideoOffer? = null
    private var autoPip: Boolean = false
    private var enteredAutoPip: Boolean = false
    private var inPipMode by mutableStateOf(false)
    private var pipPlaying by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        offer = readOffer(intent) ?: BrowserVideoOfferBus.offer.value
        autoPip = intent.getBooleanExtra(EXTRA_AUTO_PIP, false)
        if (offer == null) {
            Toast.makeText(this, "??????", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        BrowserVideoPlaybackService.stop(this)
        keepScreenOn(true)
        // SPEC-137: keep status bar so top chrome statusBarsPadding works.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    @Composable
    override fun Content() {
        val currentOffer = offer ?: return
        BrowserVideoPlayerScreen(
            offer = currentOffer,
            inPipMode = inPipMode,
            onClose = { finish() },
            onPrepared = {
                if (autoPip && !enteredAutoPip) {
                    enteredAutoPip = true
                    enterPipMode()
                }
            },
            onVideoViewReady = { vv ->
                videoView = vv
                BrowserVideoPlayerBridge.attach(vv)
            },
            onBackgroundService = { playing, title ->
                if (playing) {
                    BrowserVideoPlaybackService.start(this, title)
                } else {
                    BrowserVideoPlaybackService.stop(this)
                }
            },
            onPlayingChanged = { playing ->
                pipPlaying = playing
                updatePipActions(playing)
            },
        )
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && videoView?.isPlaying == true) {
            enterPipMode()
        }
    }

    override fun onStop() {
        super.onStop()
        if (videoView?.isPlaying == true) {
            BrowserVideoPlaybackService.start(
                this,
                offer?.title?.ifBlank { "?????" } ?: "?????",
            )
        }
    }

    override fun onDestroy() {
        videoView?.let {
            BrowserVideoPlayerBridge.detach(it)
            it.release()
        }
        videoView = null
        BrowserVideoPlaybackService.stop(this)
        keepScreenOn(false)
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            updatePipActions(videoView?.isPlaying == true)
        }
        if (autoPip && !isInPictureInPictureMode && !isFinishing) {
            finish()
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(this, "??????????", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            enterPictureInPictureMode(buildPipParams(videoView?.isPlaying == true))
        }.onFailure {
            Toast.makeText(this, "???????", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePipActions(playing: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!inPipMode && !isInPictureInPictureMode) return
        runCatching {
            setPictureInPictureParams(buildPipParams(playing))
        }
    }

    private fun buildPipParams(playing: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // SPEC-137: only play/pause; expand uses Android default (tap PiP window).
            val actionIntent = Intent(this, BrowserVideoPlaybackService::class.java)
                .setAction(
                    if (playing) {
                        BrowserVideoPlaybackService.ACTION_PAUSE
                    } else {
                        BrowserVideoPlaybackService.ACTION_PLAY
                    },
                )
            val pending = PendingIntent.getService(
                this,
                if (playing) 41 else 42,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val iconRes = if (playing) R.drawable.ic_pause else R.drawable.ic_play
            val label = if (playing) "??" else "??"
            val icon = Icon.createWithResource(this, iconRes)
            builder.setActions(
                listOf(
                    RemoteAction(icon, label, label, pending),
                ),
            )
        }
        return builder.build()
    }

    companion object {
        const val EXTRA_URL = "video_url"
        const val EXTRA_TITLE = "video_title"
        const val EXTRA_PAGE_URL = "page_url"
        const val EXTRA_HEADERS_JSON = "headers_json"
        const val EXTRA_IS_HLS = "is_hls"
        const val EXTRA_AUTO_PIP = "auto_pip"

        fun intent(
            context: android.content.Context,
            offer: BrowserVideoOffer,
            autoPip: Boolean = false,
        ): Intent {
            return Intent(context, BrowserVideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_URL, offer.url)
                putExtra(EXTRA_TITLE, offer.title)
                putExtra(EXTRA_PAGE_URL, offer.pageUrl)
                putExtra(EXTRA_IS_HLS, offer.isHls)
                putExtra(EXTRA_AUTO_PIP, autoPip)
                if (offer.headers.isNotEmpty()) {
                    putExtra(
                        EXTRA_HEADERS_JSON,
                        org.json.JSONObject(offer.headers).toString(),
                    )
                }
            }
        }

        private fun readOffer(intent: Intent): BrowserVideoOffer? {
            val url = intent.getStringExtra(EXTRA_URL)?.trim().orEmpty()
            if (url.isEmpty()) return null
            val headersRaw = intent.getStringExtra(EXTRA_HEADERS_JSON)
            return BrowserVideoOffer(
                url = url,
                headers = BrowserVideoOfferBus.parseHeaders(headersRaw),
                pageUrl = intent.getStringExtra(EXTRA_PAGE_URL).orEmpty(),
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                isHls = intent.getBooleanExtra(EXTRA_IS_HLS, url.contains(".m3u8", true)),
            )
        }
    }
}
