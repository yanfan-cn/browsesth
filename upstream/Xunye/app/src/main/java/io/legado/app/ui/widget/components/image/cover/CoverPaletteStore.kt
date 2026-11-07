package io.legado.app.ui.widget.components.image.cover

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * SPEC-164: which generated / gallery covers may be assigned when a book has no webpage image.
 */
object CoverPaletteStore {
    private const val PREFS = "browsesth_cover_palette"
    private const val KEY_RANDOM = "random_match"
    private const val KEY_ENABLED_IDS = "enabled_preset_ids"
    private const val KEY_GALLERY = "gallery_uris"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isRandomMatch(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RANDOM, true)

    fun setRandomMatch(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_RANDOM, value) }
        poolCache = null
    }

    /** Empty set while random is on means “all presets”. */
    fun enabledPresetIds(context: Context): Set<String> {
        val raw = prefs(context).getString(KEY_ENABLED_IDS, null).orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun setEnabledPresetIds(context: Context, ids: Set<String>) {
        prefs(context).edit {
            putString(KEY_ENABLED_IDS, ids.joinToString(","))
        }
        poolCache = null
    }

    fun galleryUris(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_GALLERY, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setGalleryUris(context: Context, uris: List<String>) {
        prefs(context).edit {
            putString(KEY_GALLERY, uris.joinToString("\n"))
        }
        poolCache = null
    }

    fun addGalleryUri(context: Context, uri: String) {
        val list = galleryUris(context).toMutableList()
        if (uri !in list) {
            list.add(uri)
            setGalleryUris(context, list)
        }
    }

    fun removeGalleryUri(context: Context, uri: String) {
        setGalleryUris(context, galleryUris(context).filterNot { it == uri })
    }

    /** SPEC-197: 池结果内存缓存——bookshelf 快滑时每个 item 组合都会调 pickForSeed，
     *  不缓存则每次都做 3 次 SharedPreferences 读 + 字符串切分。写操作即失效。 */
    @Volatile
    private var poolCache: List<String>? = null

    /** URIs eligible for [GeneratedCover.fromSeed]. */
    fun availablePool(context: Context): List<String> {
        poolCache?.let { return it }
        val gallery = galleryUris(context)
        val random = isRandomMatch(context)
        val enabled = enabledPresetIds(context)
        val presets = if (random || enabled.isEmpty()) {
            GeneratedCover.presets.map { it.uri }
        } else {
            GeneratedCover.presets.filter { it.id in enabled }.map { it.uri }
                .ifEmpty { GeneratedCover.presets.map { it.uri } }
        }
        return (presets + gallery).also { poolCache = it }
    }

    fun pickForSeed(context: Context, seed: String): String =
        GeneratedCover.fromSeed(seed, availablePool(context))
}
