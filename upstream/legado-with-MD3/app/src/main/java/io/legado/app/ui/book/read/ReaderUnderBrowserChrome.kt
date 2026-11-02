package io.legado.app.ui.book.read

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * SPEC-122/208: true while the reader is hosted in the tablet browser shell
 * (whether or not the browser chrome is currently visible).
 * Top inset: 16dp only when chrome is visible; fullscreen uses status-bar insets.
 */
val LocalReaderUnderBrowserChrome = staticCompositionLocalOf { false }
