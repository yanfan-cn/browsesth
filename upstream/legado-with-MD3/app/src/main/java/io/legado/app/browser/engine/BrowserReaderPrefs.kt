package io.legado.app.browser.engine

import android.content.Context

/**
 * Browser → reader auto-open policy (SPEC-015).
 * Default [TriggerMode.Manual].
 */
class BrowserReaderPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var triggerMode: TriggerMode
        get() = TriggerMode.fromStorage(prefs.getString(KEY_TRIGGER, TriggerMode.Manual.storage))
        set(value) = prefs.edit().putString(KEY_TRIGGER, value.storage).apply()

    /** SPEC-042/113: open browser ephemeral novel with translationMode; default off. */
    var autoTranslateOnOpen: Boolean
        get() = prefs.getBoolean(KEY_AUTO_TRANSLATE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_TRANSLATE, value).apply()

    enum class TriggerMode(val storage: String, val label: String) {
        Manual("manual", "手动"),
        AutoDetect("auto_detect", "自动识别"),
        SiteAuto("site_auto", "站点自动"),
        ;

        companion object {
            fun fromStorage(raw: String?): TriggerMode =
                entries.firstOrNull { it.storage == raw } ?: Manual
        }
    }

    companion object {
        private const val PREFS = "browsesth_browser_reader"
        private const val KEY_TRIGGER = "trigger_mode"
        private const val KEY_AUTO_TRANSLATE = "auto_translate_on_open"

        /** Heuristic paths for [TriggerMode.SiteAuto]. */
        fun looksLikeReaderSite(url: String): Boolean {
            val u = url.lowercase()
            if (!u.startsWith("http")) return false
            val keys = listOf(
                "/chapter", "/chapters", "/comic", "/manga", "/manhua", "/manhwa",
                "/novel", "/xiaoshuo", "/book/", "/read/", "/viewer", "/episode",
                "read.php", "chapter.php", "comic.php",
            )
            return keys.any { u.contains(it) }
        }
    }
}
