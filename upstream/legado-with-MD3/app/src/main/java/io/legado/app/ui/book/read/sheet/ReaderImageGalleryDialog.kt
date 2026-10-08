package io.legado.app.ui.book.read.sheet

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.help.book.BookHelp
import io.legado.app.model.ImageProvider
import io.legado.app.model.ReadBook
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.utils.ImageSaveUtils.saveImageToGallery
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * SPEC-057: fullscreen image gallery — swipe pages, drag down / X to dismiss,
 * long-press to share or save.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderImageGalleryDialog(
    show: Boolean,
    images: List<String>,
    initialIndex: Int,
    sourceOrigin: String? = null,
    onDismissRequest: () -> Unit,
) {
    if (!show || images.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val startIndex = initialIndex.coerceIn(0, images.lastIndex)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { images.size })
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var menuExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - (dragOffset / 600f).coerceIn(0f, 0.45f))))
                .graphicsLayer {
                    translationY = dragOffset.coerceAtLeast(0f)
                    val scale = 1f - (dragOffset / 2000f).coerceIn(0f, 0.12f)
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffset > 140f) onDismissRequest()
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            if (amount > 0f || dragOffset > 0f) {
                                dragOffset = (dragOffset + amount).coerceAtLeast(0f)
                            }
                        },
                    )
                },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val src = images[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { menuExpanded = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val model = remember(src, sourceOrigin) {
                        val file = ReadBook.book?.let { BookHelp.getImage(it, src) }
                        when {
                            file?.exists() == true -> file
                            else -> ImageRequest.Builder(context)
                                .data(src)
                                .crossfade(true)
                                .apply {
                                    sourceOrigin?.let {
                                        // Coil3 path may ignore Glide options; URL still loads.
                                    }
                                }
                                .build()
                        }
                    }
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                SymbolIcon(
                    Symbols.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White,
                )
            }

            if (menuExpanded) {
                AlertDialog(
                    onDismissRequest = { menuExpanded = false },
                    title = { Text(stringResource(R.string.menu)) },
                    text = {
                        Text(stringResource(R.string.share) + " / " + stringResource(R.string.save_image))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                menuExpanded = false
                                val src = images.getOrNull(pagerState.currentPage) ?: return@TextButton
                                scope.launch { shareImage(context, src, sourceOrigin) }
                            },
                        ) { Text(stringResource(R.string.share)) }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                menuExpanded = false
                                val src = images.getOrNull(pagerState.currentPage) ?: return@TextButton
                                scope.launch { saveImage(context, src, sourceOrigin) }
                            },
                        ) { Text(stringResource(R.string.save_image)) }
                    },
                )
            }
        }
    }
}

private suspend fun saveImage(context: android.content.Context, src: String, sourceOrigin: String?) {
    val bytes = withContext(Dispatchers.IO) { loadImageBytes(context, src, sourceOrigin) }
    val ok = bytes != null && saveImageToGallery(context, bytes)
    context.toastOnUi(
        if (ok) context.getString(R.string.save_success)
        else context.getString(R.string.error),
    )
}

private suspend fun shareImage(context: android.content.Context, src: String, sourceOrigin: String?) {
    val bytes = withContext(Dispatchers.IO) { loadImageBytes(context, src, sourceOrigin) }
    if (bytes == null) {
        // Fallback: share URL text.
        if (src.startsWith("http")) {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, src)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    context.getString(R.string.share),
                ),
            )
        } else {
            context.toastOnUi(R.string.error)
        }
        return
    }
    withContext(Dispatchers.IO) {
        val file = File(context.externalCacheDir, "share_img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        file
    }.let { file ->
        runCatching {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                AppConst.authority,
                file,
            )
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    context.getString(R.string.share),
                ),
            )
        }.onFailure {
            context.toastOnUi(R.string.error)
        }
    }
}

private fun loadImageBytes(
    context: android.content.Context,
    src: String,
    @Suppress("UNUSED_PARAMETER") sourceOrigin: String?,
): ByteArray? {
    ImageProvider.get(src)?.let { bmp ->
        return ByteArrayOutputStream().use { stream ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            stream.toByteArray()
        }
    }
    val file = ReadBook.book?.let { BookHelp.getImage(it, src) }
    if (file?.exists() == true) {
        return file.readBytes()
    }
    return runCatching {
        java.net.URL(src).openStream().use { it.readBytes() }
    }.getOrNull()
}
