package io.legado.app.browser.reader

import io.legado.app.domain.model.TranslationConstants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPageTranslateTest {

    @Test
    fun baiduPageUrl() {
        val out = WebPageTranslate.pageTranslateUrl(
            "https://example.com/a?b=1",
            TranslationConstants.PROVIDER_BAIDU,
        )
        assertTrue(out.contains("fanyi.baidu.com/transpage"))
        assertTrue(out.contains("example.com"))
    }

    @Test
    fun appAiFallsBackToBaiduPage() {
        val out = WebPageTranslate.pageTranslateUrl(
            "https://example.com/x",
            TranslationConstants.PROVIDER_APP_AI,
        )
        assertTrue(out.contains("fanyi.baidu.com/transpage"))
    }

    @Test
    fun skipsNonHttp() {
        assertTrue(
            WebPageTranslate.pageTranslateUrl("about:blank", TranslationConstants.PROVIDER_BAIDU)
                .isEmpty(),
        )
    }

    @Test
    fun detectServiceUrl() {
        assertTrue(WebPageTranslate.isTranslateServiceUrl("https://fanyi.baidu.com/transpage?query=x"))
        assertFalse(WebPageTranslate.isTranslateServiceUrl("https://example.com/"))
    }
}
