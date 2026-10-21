package io.legado.app.browser.engine

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri

/**
 * SPEC-146 R2: clipboard link detection for omnibox/NTP candidates.
 *
 * Android has no separate clipboard permission: reading while the window is
 * focused triggers the system prompt (toast on 12+, paste dialog on 13+).
 * Background reads return null on API 29+; callers must invoke this on focus
 * gained only. Content is inspected locally and never uploaded or persisted.
 */
object BrowserClipboardUrl {

    private const val MAX_LENGTH = 2048

    /** Latest clipboard text if (and only if) it looks like an http(s) URL. */
    fun latest(context: Context): String? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        val text = runCatching {
            cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
        }.getOrNull()?.trim().orEmpty()
        return parse(text)
    }

    /** Pure check, JVM-testable: returns the normalized URL or null. */
    fun parse(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH) return null
        if (trimmed.contains('\n') || trimmed.contains(' ')) return null
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return null
        }
        val host = runCatching { Uri.parse(trimmed).host }.getOrNull()
        return if (host.isNullOrBlank()) null else trimmed
    }
}