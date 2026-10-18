package io.legado.app.ui.browser.video

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.devbrackets.android.exomedia.listener.OnErrorListener
import com.devbrackets.android.exomedia.listener.OnPreparedListener
import com.devbrackets.android.exomedia.ui.widget.VideoView
import io.legado.app.browser.engine.BrowserDlnaCast
import io.legado.app.browser.engine.BrowserDownloadHelper
import io.legado.app.browser.engine.BrowserUa
import io.legado.app.browser.engine.BrowserVideoMedia
import io.legado.app.browser.engine.BrowserVideoOffer
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
    var dlnaDialog by remember { mutableStateOf(false) }
    var dlnaLoading by remember { mutableStateOf(false) }
    var dlnaDevices by remember { mutableStateOf<List<BrowserDlnaCast.Device>>(emptyList()) }
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    var windowBrightness by remember {
        mutableFloatStateOf(
            activity.window.attributes.screenBrightness.takeIf { it >= 0f } ?: 0.5f,
        )
    }
    val castSymbol = remember { MaterialSymbol("cast", autoMirror = false) }
    val rewindSymbol = remember { MaterialSymbol("replay_10", autoMirror = false) }
    val forwardSymbol = remember { MaterialSymbol("forward_10", autoMirror = false) }
    val photoSymbol = remember { MaterialSymbol("photo_camera", autoMirror = false) }
    val speedSymbol = remember { MaterialSymbol("speed", autoMirror = false) }
    val rotateSymbol = remember { MaterialSymbol("screen_rotation", autoMirror = false) }

    fun applySpeed(speed: Float, vv: VideoView?) {
        val s = speed.coerceIn(0.5f, 3.0f)
        vv?.setPlaybackSpeed(s)
        currentSpeed = s
    }

    fun seekBy(deltaMs: Long) {
        val vv = videoViewRef ?: return
        val cur = vv.currentPosition
        val dur = vv.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        vv.seekTo((cur + deltaMs).coerceIn(0L, dur))
    }

    fun toggleOrientation() {
        val landscape = activity.resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        activity.requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    LaunchedEffect(videoViewRef) {
        while (isActive) {
            val vv = videoViewRef
            if (vv != null && !scrubbing) {
                positionMs = vv.currentPosition
                durationMs = vv.duration.coerceAtLeast(0L)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).also { vv ->
                    vv.releaseOnDetachFromWindow = false
                    vv.handleAudioFocus = true
                    vv.videoControls = null
                    vv.setOnPreparedListener(OnPreparedListener {
                        vv.start()
                        playing = true
                        onPlayingChanged(true)
                        onBackgroundService(true, offer.title.ifBlank { "视频播放中" })
                        onPrepared()
                    })
                    vv.setOnErrorListener(object : OnErrorListener {
                        override fun onError(e: Exception?): Boolean {
                            errorText = "播放失败，请返回网页重试"
                            return true
                        }
                    })
                    runCatching { vv.setMedia(BrowserVideoMedia.buildSource(ctx, offer)) }
                        .onFailure { errorText = it.message ?: "无法打开视频" }
                    videoViewRef = vv
                    onVideoViewReady(vv)
                }
            },
            update = {},
        )

        if (!inPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(baseSpeed) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onLongPress = {
                                longPressing = true
                                applySpeed(BrowserVideoSpeed.LONG_PRESS_DEFAULT, videoViewRef)
                                gestureHint = BrowserVideoSpeed.label(BrowserVideoSpeed.LONG_PRESS_DEFAULT)
                            },
                            onPress = {
                                tryAwaitRelease()
                                if (longPressing) {
                                    longPressing = false
                                    applySpeed(baseSpeed, videoViewRef)
                                    gestureHint = null
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
                        var speedIdx = BrowserVideoSpeed.nearestIndex(
                            BrowserVideoSpeed.LONG_PRESS_DEFAULT,
                            BrowserVideoSpeed.GESTURE_STEPS,
                        )
                        var seekFired = false
                        detectDragGestures(
                            onDragStart = {
                                totalDx = 0f
                                totalDy = 0f
                                mode = null
                                seekFired = false
                                startVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                startBright = activity.window.attributes.screenBrightness
                                    .takeIf { it >= 0f } ?: windowBrightness
                                speedIdx = BrowserVideoSpeed.nearestIndex(
                                    if (longPressing) currentSpeed else BrowserVideoSpeed.LONG_PRESS_DEFAULT,
                                    BrowserVideoSpeed.GESTURE_STEPS,
                                )
                            },
                            onDragEnd = {
                                if (mode == GestureMode.Seek && !seekFired && abs(totalDx) > 48f) {
                                    val delta = if (totalDx > 0) SEEK_GESTURE_MS else -SEEK_GESTURE_MS
                                    seekBy(delta)
                                    gestureHint = if (delta > 0) "+15s" else "-15s"
                                }
                                if (!longPressing) gestureHint = null
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
                                        longPressing && abs(totalDx) > abs(totalDy) && abs(totalDx) > 24f ->
                                            GestureMode.Speed
                                        !longPressing && abs(totalDx) > abs(totalDy) && abs(totalDx) > 48f ->
                                            GestureMode.Seek
                                        !longPressing && abs(totalDy) > abs(totalDx) && abs(totalDy) > 16f ->
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
                                    GestureMode.Speed -> {
                                        val baseIdx = BrowserVideoSpeed.nearestIndex(
                                            BrowserVideoSpeed.LONG_PRESS_DEFAULT,
                                            BrowserVideoSpeed.GESTURE_STEPS,
                                        )
                                        val idx = (baseIdx + (totalDx / (size.width / 5f)).toInt())
                                            .coerceIn(0, BrowserVideoSpeed.GESTURE_STEPS.lastIndex)
                                        if (idx != speedIdx) {
                                            speedIdx = idx
                                            val speed = BrowserVideoSpeed.GESTURE_STEPS[idx]
                                            applySpeed(speed, videoViewRef)
                                            gestureHint = BrowserVideoSpeed.label(speed)
                                        }
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
                    .align(Alignment.Center)
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
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .height(54.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    SymbolIcon(Symbols.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = offer.title.ifBlank { Uri.parse(offer.url).host.orEmpty() },
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { toggleOrientation() }) {
                    SymbolIcon(rotateSymbol, "旋转", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                IconButton(
                    onClick = {
                        val result = BrowserDownloadHelper.enqueue(
                            context = context,
                            url = offer.url,
                            userAgent = offer.headers["User-Agent"] ?: BrowserUa.resolve(context),
                            mimeType = if (offer.isHls) "application/vnd.apple.mpegurl" else "video/mp4",
                            title = offer.title,
                            toMovies = true,
                        )
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    },
                ) {
                    SymbolIcon(Symbols.Download, "下载", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                IconButton(
                    onClick = {
                        dlnaDialog = true
                        dlnaLoading = true
                        dlnaDevices = emptyList()
                        scope.launch {
                            dlnaDevices = BrowserDlnaCast.discover(context)
                            dlnaLoading = false
                        }
                    },
                ) {
                    SymbolIcon(castSymbol, "投屏", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                val displayPos = if (scrubbing && durationMs > 0) {
                    (scrubValue * durationMs).toLong()
                } else {
                    positionMs
                }
                Text(
                    text = "${formatTime(displayPos)} / ${formatTime(durationMs)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 4.dp),
                )
                val progress = if (durationMs > 0) {
                    (if (scrubbing) scrubValue else positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Slider(
                    value = progress,
                    onValueChange = {
                        scrubbing = true
                        scrubValue = it
                    },
                    onValueChangeFinished = {
                        val vv = videoViewRef
                        if (vv != null && durationMs > 0) {
                            vv.seekTo((scrubValue * durationMs).toLong())
                        }
                        scrubbing = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        IconButton(onClick = { speedMenu = true }) {
                            SymbolIcon(speedSymbol, "倍速", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
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
                                onBackgroundService(true, offer.title.ifBlank { "视频播放中" })
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
                    IconButton(
                        onClick = {
                            val vv = videoViewRef ?: return@IconButton
                            BrowserVideoCapture.captureToPictures(vv, offer.title) { result ->
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
                    ) {
                        SymbolIcon(photoSymbol, "截图", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }

    if (dlnaDialog) {
        AlertDialog(
            onDismissRequest = { dlnaDialog = false },
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
                                                    offer.url,
                                                    offer.title,
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

private enum class GestureMode { Brightness, Volume, Speed, Seek }
