package io.legado.app.browser.engine

import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * SPEC-175: fetch suggestion words. Failures become an empty list.
 * The client has no disk cache and a short timeout, so typed text is not stored.
 */
object BrowserSearchSuggestClient {
    private const val TIMEOUT_MS = 2500L

    private val client: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .cache(null)
            .callTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
    }

    suspend fun fetch(engine: BrowserSearchEngine, query: String): List<String> {
        val url = BrowserSearchSuggest.suggestUrl(engine, query) ?: return emptyList()
        return try {
            val body = client.newCallStrResponse {
                url(url)
                header("Accept", "application/json,text/javascript,*/*")
            }.body
            if (body.isNullOrBlank()) emptyList() else BrowserSearchSuggest.parse(body)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }
}
