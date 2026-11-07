package io.legado.app.browser.engine

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * SPEC-082 R1: thin encrypted local stores for clear-browsing-data.
 * No full password-manager / autofill UI in this Spec.
 */
class BrowserPasswordStore(context: Context) {
    private val vault = BrowserVaultPrefs(context, PREFS)

    fun entries(): List<Entry> = vault.readList { o ->
        Entry(
            id = o.getString("id"),
            host = o.optString("host"),
            username = o.optString("username"),
            at = o.optLong("at"),
        )
    }

    fun countInRange(sinceMs: Long): Int =
        if (sinceMs <= 0L) entries().size else entries().count { it.at >= sinceMs }

    /** Test/seed helper — not exposed in UI this Spec. */
    fun upsert(id: String, host: String, username: String, password: String) {
        val now = System.currentTimeMillis()
        val next = entries().filterNot { it.id == id } + Entry(id, host, username, now)
        vault.writeList(next) { e ->
            JSONObject()
                .put("id", e.id)
                .put("host", e.host)
                .put("username", e.username)
                .put("at", e.at)
                .put("secret", vault.encrypt(password))
        }
    }

    data class Entry(val id: String, val host: String, val username: String, val at: Long)

    companion object {
        private const val PREFS = "browsesth_browser_passwords"
    }
}

class BrowserAutofillStore(context: Context) {
    private val vault = BrowserVaultPrefs(context, PREFS)

    fun entries(): List<Entry> = vault.readList { o ->
        Entry(
            id = o.getString("id"),
            label = o.optString("label"),
            at = o.optLong("at"),
        )
    }

    fun countInRange(sinceMs: Long): Int =
        if (sinceMs <= 0L) entries().size else entries().count { it.at >= sinceMs }

    fun upsert(id: String, label: String, value: String) {
        val now = System.currentTimeMillis()
        val next = entries().filterNot { it.id == id } + Entry(id, label, now)
        vault.writeList(next) { e ->
            JSONObject()
                .put("id", e.id)
                .put("label", e.label)
                .put("at", e.at)
                .put("secret", vault.encrypt(value))
        }
    }

    data class Entry(val id: String, val label: String, val at: Long)

    companion object {
        private const val PREFS = "browsesth_browser_autofill"
    }
}

class BrowserSiteSettingsStore(context: Context) {
    private val vault = BrowserVaultPrefs(context, PREFS)

    fun entries(): List<Entry> = vault.readList { o ->
        Entry(
            host = o.getString("host"),
            summary = o.optString("summary"),
            at = o.optLong("at"),
        )
    }

    fun countInRange(sinceMs: Long): Int =
        if (sinceMs <= 0L) entries().size else entries().count { it.at >= sinceMs }

    fun upsert(host: String, summary: String) {
        val now = System.currentTimeMillis()
        val next = entries().filterNot { it.host == host } + Entry(host, summary, now)
        vault.writeList(next) { e ->
            JSONObject()
                .put("host", e.host)
                .put("summary", e.summary)
                .put("at", e.at)
        }
    }

    data class Entry(val host: String, val summary: String, val at: Long)

    companion object {
        private const val PREFS = "browsesth_browser_site_settings"
    }
}

/** AES/GCM via AndroidKeyStore; falls back to private prefs plaintext marker if unavailable. */
private class BrowserVaultPrefs(context: Context, prefsName: String) {
    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun <T> readList(map: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(map(arr.getJSONObject(i)))
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun <T> writeList(items: List<T>, map: (T) -> JSONObject) {
        val arr = JSONArray()
        items.forEach { arr.put(map(it)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        return runCatching {
            val key = getOrCreateKey() ?: return "plain:$plain"
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val packed = cipher.iv + cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
            Base64.encodeToString(packed, Base64.NO_WRAP)
        }.getOrElse { "plain:$plain" }
    }

    private fun getOrCreateKey(): SecretKey? = runCatching {
        val ks = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return@runCatching it }
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        KeyGenerator.getInstance(
            android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE,
        ).apply { init(spec) }.generateKey()
    }.getOrNull()

    companion object {
        private const val KEY = "items"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "browsesth_browser_vault"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
