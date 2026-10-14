package io.legado.app.ui.book.read

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * SPEC-122: tablet embedded reader sits under browser omnibox — skip status-bar inset
 * on the reader top menu and use a fixed 16dp gap instead.
 */
val LocalReaderUnderBrowserChrome = staticCompositionLocalOf { false }
