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
