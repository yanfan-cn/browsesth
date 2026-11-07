package io.legado.app.ui.browser

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import io.legado.app.browser.engine.BrowserFaviconCache
import io.legado.app.browser.engine.BrowserShortcutStore
import io.legado.app.browser.engine.siteFaviconCandidates
import io.legado.app.ui.theme.LegadoTheme

/**
 * SPEC-071/221: circular site favicon with letter fallback (same chain as NTP shortcuts).
 */
@Composable
fun SiteFaviconIcon(
    pageUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    size: Dp = BrowserUiTokens.ControlSmall,
    bubbleColor: Color = LegadoTheme.colorScheme.surfaceVariant,
    letterColor: Color = LegadoTheme.colorScheme.primary,
) {
    val letter = title.trim().firstOrNull()?.uppercaseChar()?.toString()
        ?: pageUrl.trim().firstOrNull()?.uppercaseChar()?.toString()
        ?: "#"
    // SPEC-196: bundled high-res icon first (host-matched, same as NTP shortcuts),
    // then cached Chromium icon, then the shared remote candidate chain.
    val localIconRes = remember(pageUrl) {
        val host = try {
            android.net.Uri.parse(pageUrl).host.orEmpty()
        } catch (_: Throwable) {
            ""
        }
        BrowserShortcutStore.localIconResForHost(host)
    }
    val cachedBitmap = remember(pageUrl) { BrowserFaviconCache.get(pageUrl) }
    val urls = remember(pageUrl) { siteFaviconCandidates(pageUrl) }
    var index by remember(pageUrl, urls.size) { mutableIntStateOf(0) }
    val url = urls.getOrNull(index).orEmpty()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bubbleColor),
        contentAlignment = Alignment.Center,
    ) {
        if (localIconRes != null) {
            Image(
                painter = painterResource(localIconRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            return@Box
        }
        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            Image(
                bitmap = cachedBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            return@Box
        }
        Text(
            letter,
            color = letterColor,
            fontWeight = FontWeight.Medium,
            fontSize = (size.value * 0.45f).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
        )
        if (url.isNotBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .size(Size(256, 256))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { },
                error = {
                    LaunchedEffect(url) {
                        if (index < urls.lastIndex) index += 1
                    }
                },
            )
        }
    }
}
