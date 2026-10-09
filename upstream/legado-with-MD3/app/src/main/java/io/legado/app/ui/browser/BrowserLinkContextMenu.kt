package io.legado.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet

/** SPEC-093: Edge-like link/image long-press sheet. */
data class BrowserLinkContextHit(
    val href: String = "",
    val linkText: String = "",
    val imageUrl: String = "",
    val pageTitle: String = "",
    val pageUrl: String = "",
) {
    val hasLink: Boolean get() = href.startsWith("http")
    val hasImage: Boolean get() = imageUrl.startsWith("http")
}

/** Console JSON from BrowserContextMenuJs. */
data class BrowserContextMenuPayload(
    val href: String = "",
    val text: String = "",
    val img: String = "",
    val title: String = "",
    val page: String = "",
)

@Composable
fun BrowserLinkContextMenu(
    hit: BrowserLinkContextHit?,
    onDismiss: () -> Unit,
    onOpenNewTab: (String) -> Unit,
    onOpenIncognito: (String) -> Unit,
    onOpenNewWindow: (String) -> Unit,
    onCopyLink: (String) -> Unit,
    onCopyLinkText: (String) -> Unit,
    onDownloadLink: (String) -> Unit,
    onShareLink: (String, String) -> Unit,
    onDownloadImage: (String) -> Unit,
    onShareImage: (String) -> Unit,
) {
    val show = hit != null
    AppModalBottomSheet(
        show = show,
        onDismissRequest = onDismiss,
        title = null,
        contentPaddingEnabled = false,
    ) {
        if (hit == null) return@AppModalBottomSheet
        val scheme = LegadoTheme.colorScheme
        val headerTitle = hit.linkText.ifBlank {
            hit.pageTitle.ifBlank { hostOf(hit.href.ifBlank { hit.imageUrl }) }
        }
        val headerUrl = hit.href.ifBlank { hit.imageUrl }.ifBlank { hit.pageUrl }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SiteFaviconIcon(
                    pageUrl = headerUrl,
                    title = headerTitle,
                    size = 36.dp,
                    bubbleColor = scheme.surfaceVariant,
                    letterColor = scheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        headerTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp,
                        color = scheme.onSurface,
                    )
                    Text(
                        headerUrl,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
            if (hit.hasLink) {
                MenuRow("在新标签页中打开") {
                    onDismiss()
                    onOpenNewTab(hit.href)
                }
                MenuRow("在无痕浏览中打开") {
                    onDismiss()
                    onOpenIncognito(hit.href)
                }
                MenuRow("在新窗口中打开") {
                    onDismiss()
                    onOpenNewWindow(hit.href)
                }
                MenuRow("复制链接地址") {
                    onDismiss()
                    onCopyLink(hit.href)
                }
                if (hit.linkText.isNotBlank()) {
                    MenuRow("复制链接文本") {
                        onDismiss()
                        onCopyLinkText(hit.linkText)
                    }
                }
                MenuRow("下载链接") {
                    onDismiss()
                    onDownloadLink(hit.href)
                }
                MenuRow("分享链接") {
                    onDismiss()
                    onShareLink(hit.href, hit.linkText.ifBlank { hit.pageTitle })
                }
            }
            if (hit.hasImage) {
                if (hit.hasLink) {
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))
                }
                MenuRow("下载图片") {
                    onDismiss()
                    onDownloadImage(hit.imageUrl)
                }
                MenuRow("分享图片") {
                    onDismiss()
                    onShareImage(hit.imageUrl)
                }
            }
            Spacer(modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        fontSize = 16.sp,
        color = LegadoTheme.colorScheme.onSurface,
    )
}

private fun hostOf(url: String): String =
    runCatching { java.net.URI(url).host }.getOrNull()?.removePrefix("www.") ?: url.take(32)
