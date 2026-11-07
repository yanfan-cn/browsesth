package io.legado.app.browser.manga.cimoc.json

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CimocJsonRuleStoreTest {
    @Test
    fun bundledAssetJsonHasRichRules() {
        val file = File("src/main/assets/cimoc/sourceBaseUrl.json")
        assertTrue("missing bundled JSON at ${file.absolutePath}", file.isFile)
        val rich = CimocJsonRuleStore.parseJson(file.readText()).count { it.isRich }
        assertTrue("expected rich rules >= 1, got $rich", rich >= 1)
    }

    @Test
    fun parsesRichRulesFromSampleJson() {
        val json = """
            {
              "MANHUAWU": {
                "baseUrl": "https://www.mhua5.com",
                "parseChapterList1": "ul.catalog-list > li > div > a",
                "parseImageList": "ul.comic-list li.comic-page img",
                "parseImageUrl": "data-echo"
              },
              "ONLYBASE": {
                "baseUrl": "https://example.com"
              }
            }
        """.trimIndent()
        val rules = CimocJsonRuleStore.parseJson(json)
        assertTrue(rules.size == 2)
        val rich = rules.filter { it.isRich }
        assertTrue("expected >=1 rich rule", rich.size >= 1)
        assertTrue(rich.any { it.key == "MANHUAWU" })
    }

    @Test
    fun hostMatchIgnoresWww() {
        assertTrue(
            CimocJsonRuleStore.hostMatches("www.mhua5.com", "mhua5.com"),
        )
        assertTrue(
            CimocJsonRuleStore.hostMatches("m.mhua5.com", "www.mhua5.com"),
        )
    }

    @Test
    fun extractChaptersUsesList3AndPath() {
        val rule = CimocJsonRuleStore.Rule(
            key = "T",
            baseUrl = "https://example.com",
            parseChapterList1 = "div.list > a",
            parseChapterList3 = "div.more > a",
            parseChapterPath = "",
        )
        val html = """
            <div class="list"><a href="/c/1">一</a><a href="/c/2">二</a></div>
            <div class="more"><a href="/c/3">三</a></div>
        """.trimIndent()
        val ch = CimocJsonMangaBridge.extractChapters(html, "https://example.com/book", rule)
        assertTrue(ch.size >= 3)
        assertTrue(ch.any { it.title.contains("三") })
    }

    @Test
    fun extractImagesByCss() {
        val rule = CimocJsonRuleStore.Rule(
            key = "T",
            baseUrl = "https://www.mhua5.com",
            parseImageList = "ul.comic-list li.comic-page img",
            parseImageUrl = "data-echo",
        )
        val html = """
            <ul class="comic-list">
              <li class="comic-page"><img data-echo="https://cdn.example.com/a.jpg" src="loading.gif"></li>
              <li class="comic-page"><img data-echo="https://cdn.example.com/b.jpg"></li>
            </ul>
        """.trimIndent()
        val images = CimocJsonMangaBridge.extractImages(html, rule.baseUrl, rule)
        assertTrue(images.size >= 2)
        assertTrue(images.all { it.contains("cdn.example.com") })
    }
}
