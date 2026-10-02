package io.legado.app.ui.main.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.browser.engine.BrowserOpenBridge
import io.legado.app.browser.engine.BrowserSessionHolder
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.data.entities.Book
import io.legado.app.ui.browser.BrowserNtpScreen
import io.legado.app.ui.browser.BrowserSessionScreen

/**
 * Home tab: multi-tab NTP ↔ session (SPEC-012).
 */
@Composable
fun BrowserHomeTabScreen(
    onOpenBook: (Book) -> Unit,
    onSessionActiveChange: (Boolean) -> Unit = {},
    onOpenBrowserSettings: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenTabs: () -> Unit = {},
    onOpenReader: (bookUrl: String) -> Unit = {},
    onOpenMangaReader: (bookUrl: String) -> Unit = {},
) {
    val tabs by BrowserTabManager.tabs.collectAsStateWithLifecycle()
    val activeId by BrowserTabManager.activeTabId.collectAsStateWithLifecycle()
    val active = tabs.firstOrNull { it.id == activeId } ?: tabs.firstOrNull()

    val pendingOpen by BrowserOpenBridge.pendingUrl.collectAsStateWithLifecycle()
    LaunchedEffect(pendingOpen) {
        val url = pendingOpen ?: return@LaunchedEffect
        BrowserTabManager.openUrlInActive(url)
        BrowserOpenBridge.consume()
    }

    val sessionActive = active?.url != null
    LaunchedEffect(sessionActive) {
        onSessionActiveChange(sessionActive)
    }
    DisposableEffect(Unit) {
        onDispose { onSessionActiveChange(false) }
    }

    if (active == null || active.url.isNullOrBlank()) {
        BrowserNtpScreen(
            incognito = active?.incognito == true,
            onIncognitoChange = { BrowserTabManager.setActiveIncognito(it) },
            onOpenUrl = { openUrl, inIncognito ->
                BrowserTabManager.setActiveIncognito(inIncognito)
                BrowserTabManager.openUrlInActive(openUrl, inIncognito)
            },
            onOpenBook = onOpenBook,
            onOpenHistory = onOpenHistory,
            onOpenBookmarks = onOpenBookmarks,
            onOpenTabs = onOpenTabs,
        )
    } else {
        BrowserSessionScreen(
            initialUrl = active.url!!,
            showNavigationBack = true,
            onNavigateBack = {
                BrowserTabManager.clearActiveToNtp()
            },
            incognito = active.incognito,
            sessionKey = active.id,
            onOpenBrowserSettings = onOpenBrowserSettings,
            onOpenBookmarks = onOpenBookmarks,
            onOpenHistory = onOpenHistory,
            onOpenTabs = onOpenTabs,
            onOpenReader = onOpenReader,
            onOpenMangaReader = onOpenMangaReader,
            onPageMeta = { url, title ->
                BrowserTabManager.updateActiveMeta(url, title)
            },
        )
    }
}
