package io.legado.app.ui.browser.video

import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.devbrackets.android.exomedia.listener.OnErrorListener
import com.devbrackets.android.exomedia.listener.OnPreparedListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.ui.widget.VideoView
import androidx.compose.runtime.key
import io.legado.app.browser.engine.BrowserDlnaCast
import io.legado.app.browser.engine.BrowserDownloadHelper
import io.legado.app.browser.engine.BrowserUa
import io.legado.app.browser.engine.BrowserVideoMedia
import io.legado.app.browser.engine.BrowserVideoOffer
import io.legado.app.browser.engine.BrowserVideoOfferBus
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.progressIndicator.AppCircularProgressIndicator
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** SPEC-136/137: Expressive player chrome + gestures. */
@Composable
fun BrowserVideoPlayerScreen(
    offer: BrowserVideoOffer,
    inPipMode: Boolean = false,
    onClose: () -> Unit,
    onPrepared: () -> Unit = {},
    onVideoViewReady: (VideoView) -> Unit,
    onBackgroundService: (playing: Boolean, title: String) -> Unit,
    onPlayingChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as BrowserVideoPlayerActivity
    val scope = rememberCoroutineScope()
    var showControls by remember { mutableStateOf(true) }
    var gestureHint by remember { mutableStateOf<String?>(null) }
    var baseSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var longPressing by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var playing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    // SPEC-143: keep showing the scrub target until ExoPlayer's seek lands
    // (playhead within 900ms of target), else the thumb flashes back to the
    // pre-seek position for a few frames.
    var pendingSeekMs by remember { mutableLongStateOf(Long.MIN_VALUE) }
    var dlnaDialog by remember { mutableStateOf(false) }
    var dlnaLoading by remember { mutableStateOf(false) }
    var dlnaDevices by remember { mutableStateOf<List<BrowserDlnaCast.Device>>(emptyList()) }
    // SPEC-143: multi-video — sniffed list for the page + currently loaded entry.
    var playlistMenu by remember { mutableStateOf(false) }
    val playlist by BrowserVideoOfferBus.offers.collectAsState()
    var currentOffer by remember { mutableStateOf(offer) }
    // SPEC-145 R2/R3: 更多菜单 + 分辨率档位。
    var moreMenu by remember { mutableStateOf(false) }
    var qualityMenu by remember { mutableStateOf(false) }
    var qualities by remember { mutableStateOf<List<VideoQualityOption>>(emptyList()) }
    var selectedQualityHeight by remember { mutableStateOf<Int?>(null) }
    // SPEC-147 R8/R9: 视频实际宽高（onPrepared 后提取），画中画/截图按此比例。
    var videoSize by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // SPEC-152: 画面旋转步进（0/90/180/270）。平板/大屏下系统可能忽略 requestedOrientation，
    // 「旋转」改为直接旋转视频画面本身。
    var videoRotation by remember { mutableStateOf(0) }
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    var windowBrightness by remember {
        mutableFloatStateOf(
            activity.window.attributes.screenBrightness.takeIf { it >= 0f } ?: 0.5f,
        )
    }
    // SPEC-145 R1: 字体缺 replay_20/forward_20 ligature（Material Symbols 官方命名无此
    // 二者）；SPEC-147 R6 验证 forward_media/replay 字形存在，换用之；行为保持 ±20 秒。
    val rewindSymbol = remember { MaterialSymbol("replay", autoMirror = false) }
    val forwardSymbol = remember { MaterialSymbol("forward_media", autoMirror = false) }
    val playlistSymbol = remember { MaterialSymbol("playlist_play", autoMirror = false) }
    val photoSymbol = remember { MaterialSymbol("photo_camera", autoMirror = false) }
    val speedSymbol = remember { MaterialSymbol("speed", autoMirror = false) }
    val rotateSymbol = remember { MaterialSymbol("screen_rotation", autoMirror = false) }
    val pipSymbol = remember { MaterialSymbol("picture_in_picture_alt", autoMirror = false) }
    val moreSymbol = remember { MaterialSymbol("more_vert", autoMirror = false) }

    fun applySpeed(speed: Float, vv: VideoView?) {
        val s = speed.coerceIn(0.5f, 3.0f)
        vv?.setPlaybackSpeed(s)
        currentSpeed = s
    }

    /** SPEC-143: show all long-press speed steps so the slide gesture is discoverable. */
    fun speedHint(current: Float): String =
        BrowserVideoSpeed.GESTURE_STEPS.joinToString("　") { s ->
            if (s == current) "【${BrowserVideoSpeed.label(s)}】" else BrowserVideoSpeed.label(s)
        }

    fun seekBy(deltaMs: Long) {
        val vv = videoViewRef ?: return
        val cur = vv.currentPosition
        val dur = vv.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        vv.seekTo((cur + deltaMs).coerceIn(0L, dur))
    }

    /** SPEC-145 R3: prepared 后从 ExoPlayer 视频轨提取可用分辨率档位。
     * SPEC-147 R8/R9: 同时提取最高档视频宽高，回传 Activity（画中画比例）。
     * SPEC-152: 按旋转度数换算显示宽高（竖拍视频编码 1920×1080+90°，显示为 1080×1920）；
     * 拿不到视频轨尺寸时清空 videoSize（截图不做比例裁剪，宁可有黑边不裁错）。 */
    fun refreshQualities(vv: VideoView?) {
        if (vv == null) return
        var bestW = 0
        var bestH = 0
        qualities = runCatching {
            val groups = vv.availableTracks?.get(RendererType.VIDEO) ?: return@runCatching emptyList()
            val list = mutableListOf<VideoQualityOption>()
            for (g in 0 until groups.length) {
                val group = groups.get(g)
                for (t in 0 until group.length) {
                    val format = group.getFormat(t)
                    val height = format.height
                    if (height > 0) list.add(VideoQualityOption(height, g, t))
                    val rotation = runCatching { format.rotationDegrees }.getOrDefault(0)
                    val displayW = if (rotation % 180 == 0) format.width else format.height
                    val displayH = if (rotation % 180 == 0) format.height else format.width
                    if (displayW > 0 && displayH > 0 && displayW > bestW) {
                        bestW = displayW
                        bestH = displayH
                    }
                }
            }
            list.distinctBy { it.height }.sortedByDescending { it.height }
        }.getOrDefault(emptyList())
        videoSize = if (bestW > 0 && bestH > 0) {
            bestW to bestH
        } else {
            null
        }
        if (bestW > 0 && bestH > 0) {
            activity.setVideoSize(bestW, bestH)
        }
    }

    /** SPEC-145 R3: 档位切换（null = 自动）；切换后回跳原进度。 */
    fun selectQuality(option: VideoQualityOption?) {
        val vv = videoViewRef ?: return
        val resumeAt = vv.currentPosition
        runCatching {
            if (option == null) {
                vv.clearSelectedTracks(RendererType.VIDEO)
            } else {
                vv.setTrack(RendererType.VIDEO, option.groupIndex, option.trackIndex)
            }
        }.onFailure {
            Toast.makeText(context, it.message ?: "切换失败", Toast.LENGTH_SHORT).show()
            return
        }
        if (resumeAt > 0L) vv.seekTo(resumeAt)
        selectedQualityHeight = option?.height
        Toast.makeText(
            context,
            if (option == null) "已恢复自动分辨率" else "已切换至 ${option.height}P",
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun replayFromStart() {
        val vv = videoViewRef ?: return
        // SPEC-146 R6: ExoMedia's restart() re-prepares after playback ended
        // (ENDED/IDLE only); mid-pause falls back to seek+start.
        val restarted = runCatching {
            vv.restart()
        }.getOrDefault(false)
        if (!restarted) {
            vv.seekTo(0L)
            vv.start()
        }
        android.util.Log.i(
            "Diag146",
            "replay url=${currentOffer.url.take(80)} restarted=$restarted",
        )
        playing = true
        onPlayingChanged(true)
        onBackgroundService(true, currentOffer.title.ifBlank { "视频播放中" })
    }

    fun toggleOrientation() {
        // SPEC-152: 平板/大屏下系统可能忽略 requestedOrientation（原实现点了没反应）；
        // 改为旋转视频画面本身，按 90° 步进循环。
        videoRotation = (videoRotation + 90) % 360
    }

    LaunchedEffect(videoViewRef) {
        while (isActive) {
            val vv = videoViewRef
            if (vv != null && !scrubbing) {
                positionMs = vv.currentPosition
                durationMs = vv.duration.coerceAtLeast(0L)
                if (pendingSeekMs != Long.MIN_VALUE &&
                    abs(positionMs - pendingSeekMs) < 900L
                ) {
                    // SPEC-143: seek landed — hand the thumb back to the playhead.
                    pendingSeekMs = Long.MIN_VALUE
                }
                val nowPlaying = vv.isPlaying
                if (nowPlaying != playing) {
                    playing = nowPlaying
                    onPlayingChanged(nowPlaying)
                } else {
                    playing = nowPlaying
                }
            }
            delay(250)
        }
    }

    DisposableEffect(Unit) {
        onDispose { onBackgroundService(false, "") }
    }
    BackHandler { onClose() }

    // SPEC-152: 控制层隐藏（或画中画）时隐藏状态栏；显示时恢复。滑动手势可临时唤出。
    val insetsView = LocalView.current
    DisposableEffect(showControls, inPipMode, insetsView) {
        val controller = WindowCompat.getInsetsController(activity.window, insetsView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (showControls && !inPipMode) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose { }
    }
    // SPEC-152: 离开播放器（含进画中画销毁视图）时恢复状态栏，避免泄漏到其它界面。
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // SPEC-143: key by URL so picking another sniffed video tears down the
        // old player instead of feeding it a new source.
        key(currentOffer.url) {
            // SPEC-152: 换视频复位画面旋转与截图比例（feed 类连播避免用上一部的比例裁错）。
            LaunchedEffect(currentOffer.url) {
                videoRotation = 0
                videoSize = null
            }
            // SPEC-157: 保持 fillMaxSize（VideoView 按画面比例拟合），再 rotationZ；
            // 90°/270° 时用等比 scale 铺满，禁止换宽高测布局（会把画面拉扁）。
            val rotationStep = (videoRotation % 360 + 360) % 360
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = rotationStep.toFloat()
                        if (rotationStep % 180 != 0) {
                            val w = size.width
                            val h = size.height
                            if (w > 0f && h > 0f) {
                                // 旋转后原宽边贴高边，需放大 max(w/h,h/w) 才能铺满。
                                val s = maxOf(w / h, h / w)
                                scaleX = s
                                scaleY = s
                            } else {
                                scaleX = 1f
                                scaleY = 1f
                            }
                        } else {
                            // 回到 0°/180° 必须复位，否则会沿用上次 90° 的 scale。
                            scaleX = 1f
                            scaleY = 1f
                        }
                    },
                factory = { ctx ->
                    videoViewRef?.let { old ->
                        runCatching {
                            old.pause()
                            old.release()
                        }
                    }
                    videoViewRef = null
                    VideoView(ctx).also { vv ->
                        vv.releaseOnDetachFromWindow = false
                        vv.handleAudioFocus = true
                        vv.videoControls = null
                        vv.setOnPreparedListener(OnPreparedListener {
                            vv.start()
                            playing = true
                            onPlayingChanged(true)
                            onBackgroundService(true, currentOffer.title.ifBlank { "视频播放中" })
                            // SPEC-145 R3: prepared 后刷新分辨率档位。
                            refreshQualities(vv)
                            onPrepared()
                        })
                        vv.setOnErrorListener(object : OnErrorListener {
                            override fun onError(e: Exception?): Boolean {
                                errorText = "播放失败，请返回网页重试"
                                return true
                            }
                        })
                        runCatching { vv.setMedia(BrowserVideoMedia.buildSource(ctx, currentOffer)) }
                            .onFailure { errorText = it.message ?: "无法打开视频" }
                        videoViewRef = vv
                        onVideoViewReady(vv)
                    }
                },
                update = {},
            )
        }

        if (!inPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // SPEC-143: tap-only here. Long-press used to live in
                        // detectTapGestures, which consumes the whole gesture and
                        // starves the drag detectors — that's what broke
                        // long-press + slide speed control.
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                        )
                    }
                    .pointerInput(Unit) {
                        // SPEC-143: long-press starts 2x, then horizontal slide
                        // walks through 1.5x / 2x / 3x; release restores base speed.
                        var totalDx = 0f
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                totalDx = 0f
                                longPressing = true
                                applySpeed(BrowserVideoSpeed.LONG_PRESS_DEFAULT, videoViewRef)
                                gestureHint = speedHint(currentSpeed)
                            },
                            onDragEnd = {
                                longPressing = false
                                applySpeed(baseSpeed, videoViewRef)
                                gestureHint = null
                            },
                            onDragCancel = {
                                longPressing = false
                                applySpeed(baseSpeed, videoViewRef)
                                gestureHint = null
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDx += dragAmount.x
                                val baseIdx = BrowserVideoSpeed.nearestIndex(
                                    BrowserVideoSpeed.LONG_PRESS_DEFAULT,
                                    BrowserVideoSpeed.GESTURE_STEPS,
                                )
                                val idx = (baseIdx + (totalDx / (size.width / 5f)).toInt())
                                    .coerceIn(0, BrowserVideoSpeed.GESTURE_STEPS.lastIndex)
                                val speed = BrowserVideoSpeed.GESTURE_STEPS[idx]
                                if (speed != currentSpeed) {
                                    applySpeed(speed, videoViewRef)
                                    gestureHint = speedHint(speed)
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        var totalDx = 0f
                        var totalDy = 0f
                        var mode: GestureMode? = null
                        var startVol = 0
                        var startBright = windowBrightness
                        var seekFired = false
                        detectDragGestures(
                            onDragStart = {
                                totalDx = 0f
                                totalDy = 0f
                                mode = null
                                seekFired = false
                                startVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                // SPEC-143: when the window value is -1 (follow
                                // system), start from the real system brightness
                                // instead of a fixed 50%.
                                startBright = activity.window.attributes.screenBrightness
                                    .takeIf { it >= 0f }
                                    ?: runCatching {
                                        Settings.System.getInt(
                                            activity.contentResolver,
                                            Settings.System.SCREEN_BRIGHTNESS,
                                            128,
                                        ) / 255f
                                    }.getOrDefault(0.5f)
                            },
                            onDragEnd = {
                                if (mode == GestureMode.Seek && !seekFired && abs(totalDx) > 48f) {
                                    val delta = if (totalDx > 0) SEEK_GESTURE_MS else -SEEK_GESTURE_MS
                                    seekBy(delta)
                                    gestureHint = if (delta > 0) "+15s" else "-15s"
                                }
                                gestureHint = null
                                mode = null
                            },
                            onDragCancel = {
                                gestureHint = null
                                mode = null
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDx += dragAmount.x
                                totalDy += dragAmount.y
                                val x = change.position.x
                                if (mode == null) {
                                    mode = when {
                                        abs(totalDx) > abs(totalDy) && abs(totalDx) > 48f ->
                                            GestureMode.Seek
                                        abs(totalDy) > abs(totalDx) && abs(totalDy) > 16f ->
                                            if (x < size.width / 2f) GestureMode.Brightness else GestureMode.Volume
                                        else -> null
                                    }
                                }
                                when (mode) {
                                    GestureMode.Brightness -> {
                                        val next = (startBright - totalDy / size.height).coerceIn(0.01f, 1f)
                                        windowBrightness = next
                                        val lp = activity.window.attributes
                                        lp.screenBrightness = next
                                        activity.window.attributes = lp
                                        gestureHint = "亮度 ${(next * 100).toInt()}%"
                                    }
                                    GestureMode.Volume -> {
                                        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                                        val next = (startVol + (-totalDy / size.height * max).toInt())
                                            .coerceIn(0, max)
                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                        gestureHint = "音量 ${((next * 100f) / max).toInt()}%"
                                    }
                                    GestureMode.Seek -> {
                                        if (!seekFired && abs(totalDx) > 48f) {
                                            seekFired = true
                                            val delta = if (totalDx > 0) SEEK_GESTURE_MS else -SEEK_GESTURE_MS
                                            seekBy(delta)
                                            gestureHint = if (delta > 0) "+15s" else "-15s"
                                        }
                                    }
                                    null -> Unit
                                }
                            },
                        )
                    },
            )
        }

        gestureHint?.let { hint ->
            Text(
                text = hint,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    // SPEC-143: park the overlay under the top bar, not mid-screen.
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 62.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        errorText?.let { err ->
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Text(text = err, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onClose) { Text("返回", color = Color.White) }
            }
        }

        if (showControls && errorText == null && !inPipMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    // SPEC-152: 背景先于 statusBarsPadding，状态栏区域与顶栏同底色。
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .height(54.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    SymbolIcon(Symbols.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = currentOffer.title.ifBlank { Uri.parse(currentOffer.url).host.orEmpty() },
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // SPEC-145 R7: 选集菜单加宽（默认 wrap 宽度约 ~200dp 的 1.5 倍），容纳更多文字。
                if (playlist.size > 1) {
                    Box {
                        IconButton(onClick = { playlistMenu = true }) {
                            SymbolIcon(playlistSymbol, "选集", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        DropdownMenu(
                            expanded = playlistMenu,
                            onDismissRequest = { playlistMenu = false },
                            modifier = Modifier.width(300.dp),
                            // SPEC-147 R4: M3 Expressive 圆角。
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            playlist.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = item.title.ifBlank {
                                                Uri.parse(item.url).host.orEmpty()
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    },
                                    trailingIcon = {
                                        if (item.url == currentOffer.url) {
                                            SymbolIcon(Symbols.PlayArrow, "当前播放")
                                        }
                                    },
                                    onClick = {
                                        playlistMenu = false
                                        if (item.url != currentOffer.url) {
                                            playing = false
                                            positionMs = 0L
                                            durationMs = 0L
                                            pendingSeekMs = Long.MIN_VALUE
                                            errorText = null
                                            qualities = emptyList()
                                            selectedQualityHeight = null
                                            currentOffer = item
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                // SPEC-145 R2: 更多菜单（投屏/下载/分辨率/重新播放）。
                Box {
                    IconButton(onClick = { moreMenu = true }) {
                        SymbolIcon(moreSymbol, "更多", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    DropdownMenu(
                        expanded = moreMenu,
                        onDismissRequest = { moreMenu = false },
                        // SPEC-147 R4: M3 Expressive 圆角。
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        DropdownMenuItem(
                            text = { Text("投屏") },
                            onClick = {
                                moreMenu = false
                                dlnaDialog = true
                                dlnaLoading = true
                                dlnaDevices = emptyList()
                                scope.launch {
                                    dlnaDevices = BrowserDlnaCast.discover(context)
                                    dlnaLoading = false
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("下载") },
                            onClick = {
                                moreMenu = false
                                val result = BrowserDownloadHelper.enqueue(
                                    context = context,
                                    url = currentOffer.url,
                                    userAgent = currentOffer.headers["User-Agent"] ?: BrowserUa.resolve(context),
                                    mimeType = if (currentOffer.isHls) "application/vnd.apple.mpegurl" else "video/mp4",
                                    title = currentOffer.title,
                                    toMovies = true,
                                    headers = currentOffer.headers,
                                    // SPEC-147 R3: headers 无 Referer 时回退来源页，
                                    // 防盗链站缺 Referer 会 403。
                                    referer = currentOffer.pageUrl,
                                )
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (selectedQualityHeight == null) {
                                        "分辨率 · 自动"
                                    } else {
                                        "分辨率 · ${selectedQualityHeight}P"
                                    },
                                )
                            },
                            onClick = {
                                moreMenu = false
                                qualityMenu = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("重新播放") },
                            onClick = {
                                moreMenu = false
                                replayFromStart()
                            },
                        )
                    }
                    // SPEC-147 R2: 清晰度菜单从底栏迁到「更多」锚点。
                    DropdownMenu(
                        expanded = qualityMenu,
                        onDismissRequest = { qualityMenu = false },
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        if (qualities.size <= 1) {
                            // SPEC-145 拍板：MP4 单档显示「自动」+提示。
                            DropdownMenuItem(
                                text = { Text("当前视频仅一种清晰度") },
                                onClick = { qualityMenu = false },
                                enabled = false,
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("自动") },
                                trailingIcon = {
                                    if (selectedQualityHeight == null) {
                                        SymbolIcon(Symbols.Check, "当前")
                                    }
                                },
                                onClick = {
                                    qualityMenu = false
                                    selectQuality(null)
                                },
                            )
                            qualities.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("${option.height}P") },
                                    trailingIcon = {
                                        if (selectedQualityHeight == option.height) {
                                            SymbolIcon(Symbols.Check, "当前")
                                        }
                                    },
                                    onClick = {
                                        qualityMenu = false
                                        selectQuality(option)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // SPEC-145 R5: 右侧中部纵向侧栏（右边距 24dp）：截图 + 旋转。
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(
                    onClick = {
                        val vv = videoViewRef ?: return@IconButton
                        BrowserVideoCapture.captureToPictures(vv, currentOffer.title, videoAspect = videoSize) { result ->
                            result.onSuccess {
                                Toast.makeText(context, "截图已保存", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    it.message ?: "截图失败",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                ) {
                    SymbolIcon(photoSymbol, "截图", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                IconButton(
                    onClick = { toggleOrientation() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                ) {
                    SymbolIcon(rotateSymbol, "旋转", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // SPEC-152: 背景先于 navigationBarsPadding，导航栏区域与底栏同底色。
                    .background(Color.Black.copy(alpha = 0.45f))
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                // SPEC-147 R2: 删除进度条上方整行（清晰度/时间）；时间移到进度条右侧；
                // 分辨率入口保留在「更多」菜单。底栏纵向内边距收紧更紧凑。
                val displayPos = when {
                    scrubbing && durationMs > 0 -> (scrubValue * durationMs).toLong()
                    pendingSeekMs != Long.MIN_VALUE -> pendingSeekMs
                    else -> positionMs
                }
                val progress = when {
                    durationMs <= 0 -> 0f
                    scrubbing -> scrubValue
                    pendingSeekMs != Long.MIN_VALUE ->
                        (pendingSeekMs.toFloat() / durationMs).coerceIn(0f, 1f)
                    else -> (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Slider(
                        value = progress,
                        onValueChange = {
                            scrubbing = true
                            scrubValue = it
                            pendingSeekMs = Long.MIN_VALUE
                        },
                        onValueChangeFinished = {
                            val vv = videoViewRef
                            if (vv != null && durationMs > 0) {
                                val target = (scrubValue * durationMs).toLong()
                                vv.seekTo(target)
                                // SPEC-143: hold the thumb at the target until the
                                // playhead catches up, so it doesn't flash back.
                                pendingSeekMs = target
                            }
                            scrubbing = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        ),
                    )
                    Text(
                        text = "${formatTime(displayPos)} / ${formatTime(durationMs)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        IconButton(onClick = { speedMenu = true }) {
                            SymbolIcon(speedSymbol, "倍速", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }, shape = RoundedCornerShape(24.dp)) {
                            BrowserVideoSpeed.STEPS.forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text(BrowserVideoSpeed.label(speed)) },
                                    onClick = {
                                        baseSpeed = speed
                                        applySpeed(speed, videoViewRef)
                                        speedMenu = false
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { seekBy(-20_000L) }) {
                        SymbolIcon(rewindSymbol, "后退20秒", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(
                        onClick = {
                            val vv = videoViewRef ?: return@IconButton
                            if (vv.isPlaying) {
                                vv.pause()
                                playing = false
                                onPlayingChanged(false)
                            } else {
                                vv.start()
                                playing = true
                                onPlayingChanged(true)
                                onBackgroundService(true, currentOffer.title.ifBlank { "视频播放中" })
                            }
                        },
                    ) {
                        SymbolIcon(
                            if (playing) Symbols.Pause else Symbols.PlayArrow,
                            if (playing) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    IconButton(onClick = { seekBy(20_000L) }) {
                        SymbolIcon(forwardSymbol, "前进20秒", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    // SPEC-145 R4: 右下角截图键 → 画中画键（截图移至右侧侧栏）。
                    IconButton(onClick = { activity.enterPipMode() }) {
                        SymbolIcon(pipSymbol, "画中画", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }

    if (dlnaDialog) {
        AlertDialog(
            onDismissRequest = { dlnaDialog = false },
            // SPEC-147 R4: M3 Expressive 圆角。
            shape = RoundedCornerShape(24.dp),
            title = { Text("投屏到电视") },
            text = {
                when {
                    dlnaLoading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppCircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("正在搜索 DLNA 设备…")
                        }
                    }
                    dlnaDevices.isEmpty() -> Text("未发现可用设备，请确认电视与手机在同一 Wi-Fi")
                    else -> {
                        Column {
                            dlnaDevices.forEach { device ->
                                Text(
                                    text = device.friendlyName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                val r = BrowserDlnaCast.castUrl(
                                                    device,
                                                    currentOffer.url,
                                                    currentOffer.title,
                                                )
                                                dlnaDialog = false
                                                val msg = if (r.isSuccess) {
                                                    "已发送到 ${device.friendlyName}"
                                                } else {
                                                    val err = r.exceptionOrNull()?.message ?: "未知错误"
                                                    "投屏失败：$err"
                                                }
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dlnaDialog = false }) { Text("关闭") }
            },
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000L
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}

private const val SEEK_GESTURE_MS = 15_000L

/** SPEC-145 R3: 分辨率档位（height 为显示值，group/track 定位 ExoPlayer 轨道）。 */
data class VideoQualityOption(val height: Int, val groupIndex: Int, val trackIndex: Int)

private enum class GestureMode { Brightness, Volume, Seek }
