package io.legado.app.ui.browser.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
    // SPEC-147 R8: 实际视频宽高（onPrepared 后由 Screen 回传），用于画中画窗口比例。
    internal var videoAspectRatio: Rational? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        offer = readOffer(intent) ?: BrowserVideoOfferBus.offer.value
        autoPip = intent.getBooleanExtra(EXTRA_AUTO_PIP, false)
        if (offer == null) {
            Toast.makeText(this, "无法打开视频", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        activeInstance = this
        BrowserWebMediaBridge.pauseAll()
        BrowserVideoPlaybackService.stop(this)
        keepScreenOn(true)
        applyRotationPolicy()
    }

    override fun onResume() {
        super.onResume()
        // SPEC-143: honor the system auto-rotate toggle every time we come back.
        applyRotationPolicy()
    }

    /** SPEC-143: follow the system "auto-rotate" setting instead of always spinning. */
    private fun applyRotationPolicy() {
        val systemAutoRotate = runCatching {
            Settings.System.getInt(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0,
            ) == 1
        }.getOrDefault(true)
        requestedOrientation = if (systemAutoRotate) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
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
        // SPEC-188: do not auto-enter PiP on Home; leave-to-stop handles background.
        super.onUserLeaveHint()
    }

    override fun onStop() {
        super.onStop()
        // SPEC-188: leaving the player (Home / multi-task) stops video unless in PiP.
        if (isChangingConfigurations) return
        if (inPipMode || isInPictureInPictureMode) {
            if (videoView?.isPlaying == true) {
                BrowserVideoPlaybackService.start(
                    this,
                    offer?.title?.ifBlank { "视频播放中" } ?: "视频播放中",
                )
            }
            return
        }
        runCatching { videoView?.pause() }
        BrowserVideoPlaybackService.stop(this)
        if (!isFinishing) {
            finish()
        }
    }

    override fun onDestroy() {
        if (activeInstance === this) {
            activeInstance = null
        }
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

    /** SPEC-147 R8: Screen 在 onPrepared 后回传视频轨尺寸（0/0 表示未知，保持 16:9）。 */
    internal fun setVideoSize(width: Int, height: Int) {
        videoAspectRatio = if (width > 0 && height > 0) {
            Rational(width, height)
        } else {
            null
        }
    }

    // SPEC-145 R4: 播放器 Screen 需要触发 PIP。
    internal fun enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(this, "??????????", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            enterPictureInPictureMode(buildPipParams(videoView?.isPlaying == true, videoAspectRatio))
        }.onFailure {
            Toast.makeText(this, "???????", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePipActions(playing: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!inPipMode && !isInPictureInPictureMode) return
        runCatching {
            setPictureInPictureParams(buildPipParams(playing, videoAspectRatio))
        }
    }

    private fun buildPipParams(playing: Boolean, aspect: Rational? = null): PictureInPictureParams {
        // SPEC-147 R8: 按视频实际宽高比自适应画中画窗口；未知回退 16:9。
        // 系统限制：宽高比须在约 0.418410~2.39 之间，越界参数会被系统忽略。
        val rational = aspect ?: Rational(16, 9)
        val safe = when {
            rational.toDouble() > 2.39 -> Rational(239, 100)
            rational.toDouble() < 0.418410 -> Rational(41841, 100000)
            else -> rational
        }
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(safe)
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

        @Volatile
        private var activeInstance: BrowserVideoPlayerActivity? = null

        /** SPEC-188: leave-to-stop closes the native video player (not audio-only). */
        fun finishIfShowingVideo() {
            val act = activeInstance ?: return
            act.runOnUiThread {
                runCatching { act.videoView?.pause() }
                if (!act.isFinishing) {
                    act.finish()
                }
            }
        }

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
