package io.legado.app.ui.book.manga

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.base.BaseComposeActivity
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.info.READER_RESULT_DELETED
import io.legado.app.ui.book.read.sheet.ReaderBookSheetRoute
import io.legado.app.ui.book.read.sheet.ReaderBookSheetTab
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.login.SourceLoginType
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.StartActivityContract
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * SPEC-190: 独立 Activity 宿主的漫画阅读器。
 *
 * 阅读器不再压在 MainActivity 的 nav3 栈上：进入/退出转场交给系统窗口动画，
 * MainActivity（网页/阅读列表/详情）作为底页全程保持渲染、不重组。
 *
 * 本 Activity 只负责 Android 层：窗口状态、音量键翻页、Activity 结果（书籍信息/书源编辑）、
 * 二级目的地跳转。阅读界面与业务状态在 [MangaReaderRouteScreen] / [MangaReaderViewModel]。
 */
class ReadMangaActivity : BaseComposeActivity(imageBg = false), MangaReaderKeyHost {

    private val readerViewModel by viewModel<MangaReaderViewModel>()

    override var activeMangaKeyHandler: ((Int) -> Boolean)? = null

    /**
     * RouteScreen 以 openRequestId 区分同一本书的重复打开请求（外部目录选章后回开）；
     * onNewIntent 复用实例时递增它触发重新 Initialize。
     */
    private var openRequestId by mutableLongStateOf(0L)

    private val sourceEditActivity =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                readerViewModel.onIntent(MangaReaderIntent.RefreshBookSource)
            }
        }

    private val bookInfoActivity =
        registerForActivityResult(StartActivityContract(BookInfoActivity::class.java)) {
            if (it.resultCode == READER_RESULT_DELETED) {
                finish()
            } else {
                readerViewModel.onIntent(MangaReaderIntent.ReloadContent)
            }
        }

    private val tocActivity = registerForActivityResult(TocActivityResult()) { result ->
        result?.let { (chapterIndex, chapterPos, _) ->
            readerViewModel.onIntent(MangaReaderIntent.OpenChapter(chapterIndex, chapterPos))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openRequestId = if (savedInstanceState != null) {
            savedInstanceState.getLong(STATE_OPEN_REQUEST_ID)
        } else {
            System.nanoTime()
        }
    }

    override fun onResume() {
        super.onResume()
        io.legado.app.ui.main.ReaderForegroundTracker.readerActivityInForeground = true
    }

    override fun onPause() {
        io.legado.app.ui.main.ReaderForegroundTracker.readerActivityInForeground = false
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_OPEN_REQUEST_ID, openRequestId)
    }

    @Composable
    override fun Content() {
        val state by readerViewModel.uiState.collectAsStateWithLifecycle()
        MangaReaderRouteScreen(
            bookUrl = intent.getStringExtra("bookUrl"),
            inBookshelf = intent.getBooleanExtra("inBookshelf", true),
            chapterChanged = intent.getBooleanExtra("chapterChanged", false),
            openRequestId = openRequestId,
            viewModel = readerViewModel,
            restoreSystemBarsVisible = true,
            onFinish = { bookshelfChanged ->
                if (bookshelfChanged) setResult(RESULT_OK)
                finish()
            },
            onOpenBookInfo = { name, author, bookUrl ->
                if (bookUrl.isNotEmpty()) {
                    bookInfoActivity.launch {
                        putExtra("name", name)
                        putExtra("author", author)
                        putExtra("bookUrl", bookUrl)
                    }
                }
            },
            onOpenSourceLogin = { sourceUrl ->
                startActivity(
                    MainActivity.createSourceLoginIntent(
                        this,
                        SourceLoginType.BookSource,
                        sourceUrl,
                    )
                )
            },
            onOpenSourceEdit = { sourceUrl ->
                sourceEditActivity.launch(
                    MainActivity.createBookSourceEditIntent(this, sourceUrl)
                )
            },
            onOpenWebView = { title, url, sourceOrigin, sourceName, sourceType ->
                startActivity(
                    MainActivity.createWebViewIntent(
                        this,
                        title,
                        url,
                        sourceOrigin,
                        sourceName,
                        sourceType,
                    )
                )
            },
        )
        if (state.activeSheet == MangaReaderSheet.Catalog && state.bookUrl.isNotEmpty()) {
            ReaderBookSheetRoute(
                show = true,
                bookUrl = state.bookUrl,
                initialTab = ReaderBookSheetTab.Toc,
                currentChapterIndex = state.pendingChapterIndex ?: state.chapterIndex,
                onDismissRequest = { readerViewModel.onIntent(MangaReaderIntent.DismissSheet) },
                onChapterClick = { chapterIndex, pageIndex ->
                    readerViewModel.onIntent(MangaReaderIntent.DismissSheet)
                    readerViewModel.onIntent(MangaReaderIntent.OpenChapter(chapterIndex, pageIndex))
                },
                onOpenFullBookInfo = {
                    readerViewModel.onIntent(MangaReaderIntent.DismissSheet)
                    openBookInfoActivity()
                },
                onOpenFullToc = { tocActivity.launch(state.bookUrl) },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequestId = System.nanoTime()
    }

    private fun openBookInfoActivity() {
        readerViewModel.uiState.value.let {
            if (it.bookUrl.isEmpty()) return
            bookInfoActivity.launch {
                putExtra("name", it.bookName)
                putExtra("author", it.bookAuthor)
                putExtra("bookUrl", it.bookUrl)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (activeMangaKeyHandler?.invoke(keyCode) == true) return true
        return super.onKeyDown(keyCode, event)
    }

    private companion object {
        const val STATE_OPEN_REQUEST_ID = "readManga.openRequestId"
    }
}
