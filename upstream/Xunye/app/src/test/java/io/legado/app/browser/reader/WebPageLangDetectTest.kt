package io.legado.app.browser.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPageLangDetectTest {

    @Test
    fun simplifiedLangTag_noPrompt() {
        assertFalse(
            WebPageLangDetect.shouldPrompt(
                "zh-CN",
                "这是一段简体中文小说正文内容，用来测试语言识别是否正确工作。",
            ),
        )
    }

    @Test
    fun englishSample_prompts() {
        assertTrue(
            WebPageLangDetect.shouldPrompt(
                "en",
                "Once upon a time there was a little princess who lived in a castle by the sea. " +
                    "She loved to read books every afternoon when the sun was warm.",
            ),
        )
    }

    @Test
    fun traditionalLangTag_prompts() {
        assertTrue(
            WebPageLangDetect.shouldPrompt("zh-TW", "這是一段繁體中文的測試文字內容，用於判斷是否需要翻譯。"),
        )
    }
}
