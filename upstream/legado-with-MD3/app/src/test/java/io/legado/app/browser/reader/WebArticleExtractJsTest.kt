package io.legado.app.browser.reader

import com.script.rhino.RhinoScriptEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebArticleExtractJsTest {

    @Test
    fun neteaseLazyReloadImageIsKept() {
        val html = decide(
            cls = "image-lazy image-reload",
            dataSrc = "https://dingyue.ws.126.net/2024/0910/abc123.jpg",
            src = "data:image/gif;base64,AAAA",
        )
        assertEquals(
            """<img src="https://dingyue.ws.126.net/2024/0910/abc123.jpg">""",
            html,
        )
    }

    @Test
    fun uploadAndReadPathsAreNotTreatedAsAds() {
        val html = decide(
            cls = "photo",
            dataSrc = "",
            src = "https://cdn.example.com/upload/read/photo.jpg",
            width = 800,
            height = 600,
        )
        assertEquals(
            """<img src="https://cdn.example.com/upload/read/photo.jpg">""",
            html,
        )
    }

    @Test
    fun adClassAndAdUrlAreStillDropped() {
        assertEquals(
            "",
            decide(cls = "ad", dataSrc = "", src = "https://cdn.example.com/pics/a.jpg", width = 300, height = 200),
        )
        assertEquals(
            "",
            decide(cls = "ads", dataSrc = "", src = "https://cdn.example.com/pics/a.jpg", width = 300, height = 200),
        )
        assertEquals(
            "",
            decide(
                cls = "advertisement",
                dataSrc = "",
                src = "https://cdn.example.com/pics/a.jpg",
                width = 300,
                height = 200,
            ),
        )
        assertEquals(
            "",
            decide(
                cls = "photo",
                dataSrc = "",
                src = "https://cdn.example.com/ads/banner.jpg",
                width = 300,
                height = 200,
            ),
        )
    }

    @Test
    fun smallThumbnailIsDropped() {
        assertEquals(
            "",
            decide(cls = "photo", dataSrc = "", src = "https://cdn.example.com/pics/t.jpg", width = 48, height = 48),
        )
        assertEquals(
            "",
            decide(
                cls = "photo",
                dataSrc = "https://cdn.example.com/pics/lazy-thumb.jpg",
                src = "https://cdn.example.com/pics/lazy-thumb.jpg",
                width = 400,
                height = 300,
                attrWidth = "64",
                attrHeight = "64",
            ),
        )
    }

    @Test
    fun lazyPlaceholderSizeDoesNotDropTheImage() {
        val html = decide(
            cls = "content",
            dataSrc = "https://dingyue.ws.126.net/2024/0910/wide.jpg",
            src = "https://example.com/1x1.gif",
            width = 1,
            height = 1,
        )
        assertEquals(
            """<img src="https://dingyue.ws.126.net/2024/0910/wide.jpg">""",
            html,
        )
    }

    @Test
    fun scriptUsesBoundedAdToken() {
        val filter = WebArticleExtractJs.imageFilterJs
        assertFalse(filter.contains("|ads?"))
        assertFalse(filter.contains("ads?|"))
        assertTrue(filter.contains("(?:^|[^a-z0-9])ads?(?![a-z0-9])"))
        assertTrue(WebArticleExtractJs.SCRIPT.contains(filter))
    }

    @Test
    fun prefersPortalArticleRootSelectors() {
        // SPEC-211: align extract root with hydration probe selectors.
        val script = WebArticleExtractJs.SCRIPT
        assertTrue(script.contains("#ArticleContent"))
        assertTrue(script.contains(".content-article"))
        assertTrue(script.contains(".qq-article"))
        assertTrue(script.contains(".article-content"))
        val portalIdx = script.indexOf("#ArticleContent")
        val articleIdx = script.indexOf("querySelector('article')")
        assertTrue(portalIdx in 0 until articleIdx)
    }

    @Test
    fun strongExtractAddsRootsAndChromeHints() {
        // SPEC-217: default script lacks strong-only tokens; strong script includes them.
        val base = WebArticleExtractJs.script(strongExtract = false)
        val strong = WebArticleExtractJs.script(strongExtract = true)
        assertFalse(base.contains("rich_media_content"))
        assertFalse(base.contains("open-app"))
        assertTrue(strong.contains("rich_media_content"))
        assertTrue(strong.contains("#js_content"))
        assertTrue(strong.contains("open-app"))
        assertTrue(strong.contains("download-app"))
        assertTrue(strong.contains("askai") || strong.contains("ai-ask"))
        val portalIdx = strong.indexOf("#ArticleContent")
        val articleIdx = strong.indexOf("querySelector('article')")
        assertTrue(portalIdx in 0 until articleIdx)
    }

    @Test
    fun skipsMediaTagsAndEmitsJingshiVideoThumb() {
        // SPEC-212
        val script = WebArticleExtractJs.SCRIPT
        assertTrue(script.contains("tag==='video'"))
        assertTrue(script.contains("tag==='audio'"))
        assertTrue(script.contains("tag==='source'"))
        assertTrue(script.contains("#browsesth-jingshi"))
        assertTrue(script.contains("browsesth-jingshi://pending"))
        assertTrue(script.contains("txp-") || script.contains("tenvideo"))
        assertTrue(script.contains("player-info") || script.contains("video-info"))
    }

    @Test
    fun dropsQrUrlAndNearSquareWidgetImages() {
        assertEquals(
            "",
            decide(
                cls = "photo",
                dataSrc = "",
                src = "https://cdn.example.com/qrcode/mobile.png",
                width = 600,
                height = 400,
            ),
        )
        assertEquals(
            "",
            decide(
                cls = "photo",
                dataSrc = "",
                src = "https://cdn.example.com/mascot.png",
                width = 240,
                height = 240,
            ),
        )
        assertEquals(
            """<img src="https://cdn.example.com/story.jpg">""",
            decide(
                cls = "photo",
                dataSrc = "",
                src = "https://cdn.example.com/story.jpg",
                width = 800,
                height = 450,
            ),
        )
    }

    private fun decide(
        cls: String,
        dataSrc: String,
        src: String,
        width: Int = 0,
        height: Int = 0,
        attrWidth: String = "",
        attrHeight: String = "",
    ): String {
        val js = """
            var document = { baseURI: 'https://www.163.com/news/article/ABC.html' };
            ${WebArticleExtractJs.imageFilterJs}
            function decide(cls, dataSrc, src, w, h, attrW, attrH) {
              var el = {
                className: cls,
                id: '',
                getAttribute: function(n) {
                  if (n === 'data-src') return dataSrc || '';
                  if (n === 'width') return attrW || '';
                  if (n === 'height') return attrH || '';
                  return '';
                },
                currentSrc: src || '',
                src: src || '',
                srcset: '',
                naturalWidth: w || 0,
                naturalHeight: h || 0,
                width: w || 0,
                height: h || 0
              };
              return imgTag(el);
            }
            decide(${jsString(cls)}, ${jsString(dataSrc)}, ${jsString(src)}, $width, $height, ${jsString(attrWidth)}, ${jsString(attrHeight)});
        """.trimIndent()
        return RhinoScriptEngine.eval(js).toString()
    }

    private fun jsString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
