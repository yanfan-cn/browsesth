package io.legado.app.ui.main

import android.app.Application
import android.content.Intent
import io.legado.app.ui.book.read.ReadBookActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class MainIntentTest {

    @Test
    fun `media control intent targets the reader activity with read aloud`() {
        val context: Application = RuntimeEnvironment.getApplication()

        val intent = MainIntent.createReadBookMediaControlIntent(context)

        assertEquals(ReadBookActivity::class.java.name, intent.component?.className)
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            intent.flags and (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        assertTrue(intent.getBooleanExtra(MainIntent.EXTRA_READ_ALOUD, false))
        assertFalse(intent.hasExtra(MainIntent.EXTRA_START_ROUTE))
    }

    @Test
    fun `reader intents target dedicated reader activities not MainActivity`() {
        val context: Application = RuntimeEnvironment.getApplication()

        val readBook = MainIntent.createReadBookIntent(context, readAloud = true)
        val readManga = MainIntent.createReadMangaIntent(context, bookUrl = "book")

        assertEquals(ReadBookActivity::class.java.name, readBook.component?.className)
        assertTrue(readBook.getBooleanExtra(MainIntent.EXTRA_READ_ALOUD, false))
        assertEquals(
            io.legado.app.ui.book.manga.ReadMangaActivity::class.java.name,
            readManga.component?.className,
        )
        assertEquals("book", readManga.getStringExtra(MainIntent.EXTRA_BOOK_URL))
        // SPEC-190: reader intents never route through MainActivity's start-route plumbing.
        assertFalse(readBook.hasExtra(MainIntent.EXTRA_START_ROUTE))
        assertFalse(readManga.hasExtra(MainIntent.EXTRA_START_ROUTE))
    }

    @Test
    fun `home intent opens main route not documentation example domain`() {
        val context: Application = RuntimeEnvironment.getApplication()

        val intent = MainIntent.createHomeIntent(context)

        assertEquals(
            MainRouteConst.ROUTE_MAIN,
            intent.getStringExtra(MainIntent.EXTRA_START_ROUTE),
        )
        val url = intent.getStringExtra(MainIntent.EXTRA_WEB_VIEW_URL)
        assertTrue(url.isNullOrBlank() || !url.contains("example.com"))
        assertTrue(
            intent.getStringExtra(MainIntent.EXTRA_START_ROUTE) != MainRouteConst.ROUTE_WEB_VIEW,
        )
    }

    @Test
    fun `search content intent carries reader search parameters`() {
        val context: Application = RuntimeEnvironment.getApplication()

        val intent = MainIntent.createSearchContentIntent(
            context,
            bookUrl = "book",
            searchWord = "word",
            searchResultIndex = 3,
            autoFocus = false,
        )

        assertEquals(MainRouteConst.ROUTE_SEARCH_CONTENT, intent.getStringExtra(MainIntent.EXTRA_START_ROUTE))
        assertEquals("book", intent.getStringExtra(MainIntent.EXTRA_BOOK_URL))
        assertEquals("word", intent.getStringExtra(MainIntent.EXTRA_SEARCH_KEY))
        assertEquals(3, intent.getIntExtra(MainIntent.EXTRA_SEARCH_RESULT_INDEX, 0))
        assertFalse(intent.getBooleanExtra(MainIntent.EXTRA_SEARCH_AUTO_FOCUS, true))
    }
}
