package io.legado.app.ui.browser

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.legado.app.ui.book.manga.MangaReaderRouteScreen
import io.legado.app.ui.book.manga.MangaReaderViewModel
import io.legado.app.ui.book.read.ReadBookController
import io.legado.app.ui.book.read.ReadBookInitRequest
import io.legado.app.ui.book.read.ReadBookIntent
import io.legado.app.ui.book.read.ReadBookRouteScreen
import io.legado.app.ui.book.read.ReadBookViewModel
import io.legado.app.ui.book.read.ReaderSessionViewModel
import io.legado.app.ui.main.MainActivity
import io.legado.app.ui.theme.LocalAppUiConfiguration
import io.legado.app.ui.widget.components.privacy.PrivateReadGate
import kotlinx.coroutines.CompletableDeferred
import org.koin.androidx.compose.koinViewModel

enum class TabletEmbeddedReaderKind { Novel, Manga }

/**
 * SPEC-036: keep tablet browser chrome by hosting reader inside the shell content slot.
 */
@Composable
fun TabletEmbeddedReaderHost(
    kind: TabletEmbeddedReaderKind,
    bookUrl: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
    when (kind) {
        TabletEmbeddedReaderKind.Novel -> TabletEmbeddedNovelReader(
            bookUrl = bookUrl,
            onClose = onClose,
        )
        TabletEmbeddedReaderKind.Manga -> TabletEmbeddedMangaReader(
            bookUrl = bookUrl,
            onClose = onClose,
        )
    }
    }
}

@Composable
private fun TabletEmbeddedNovelReader(
    bookUrl: String,
    onClose: () -> Unit,
) {
    val activity = LocalActivity.current as MainActivity
    PrivateReadGate(
        bookUrl = bookUrl,
        onExit = onClose,
        onOpenLocalPasswordSettings = onClose,
    ) {
        val readBookViewModel = koinViewModel<ReadBookViewModel>(
            key = "ReadBook:tablet:$bookUrl",
        )
        val readerSessionViewModel = koinViewModel<ReaderSessionViewModel>(
            key = "ReaderSession:tablet:$bookUrl",
        )
        val controller = remember(readBookViewModel, readerSessionViewModel) {
            ReadBookController(activity, readBookViewModel, readerSessionViewModel)
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        val initRequest = remember(bookUrl) {
            ReadBookInitRequest(bookUrl = bookUrl, inBookshelf = false)
        }
        val effectsReady = remember(readBookViewModel) { CompletableDeferred<Unit>() }
        val readerResumeState = remember(controller, lifecycleOwner) { booleanArrayOf(false) }
        val collectorReady = remember(readBookViewModel) { booleanArrayOf(false) }

        fun resumeReader() {
            if (readerResumeState[0]) return
            readerResumeState[0] = true
            controller.onResume()
            readBookViewModel.onIntent(ReadBookIntent.OnResume)
        }

        fun pauseReader() {
            if (!readerResumeState[0]) return
            readerResumeState[0] = false
            readBookViewModel.onIntent(ReadBookIntent.OnPause)
            controller.onPause()
        }

        Box(Modifier.fillMaxSize()) {
            ReadBookRouteScreen(
                viewModel = readBookViewModel,
                readerSessionViewModel = readerSessionViewModel,
                host = controller,
                controller = controller,
                onEffectsReady = { effectsReady.complete(Unit) },
            )
        }

        DisposableEffect(controller, lifecycleOwner, bookUrl) {
            activity.activeReadBookInputHandler = controller
            MainActivity.hasActiveReadBookRoute = true
            controller.onClose = onClose
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        if (collectorReady[0]) resumeReader()
                    }
                    Lifecycle.Event.ON_PAUSE -> pauseReader()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            onDispose {
                pauseReader()
                readBookViewModel.onIntent(ReadBookIntent.OnDispose)
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                if (activity.activeReadBookInputHandler === controller) {
                    activity.activeReadBookInputHandler = null
                }
                MainActivity.hasActiveReadBookRoute = false
                controller.clearTts()
            }
        }

        LaunchedEffect(bookUrl, readBookViewModel, lifecycleOwner) {
            val initialBook = readBookViewModel.initReadBookConfig(initRequest)
            effectsReady.await()
            collectorReady[0] = true
            readBookViewModel.initData(initRequest, initialBook) {
                readBookViewModel.markJustInitData()
                controller.onRouteInitialized()
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    resumeReader()
                }
            }
        }
    }
}

@Composable
private fun TabletEmbeddedMangaReader(
    bookUrl: String,
    onClose: () -> Unit,
) {
    val mangaViewModel = koinViewModel<MangaReaderViewModel>(
        key = "ReadManga:tablet:$bookUrl",
    )
    val appUi = LocalAppUiConfiguration.current
    MangaReaderRouteScreen(
        bookUrl = bookUrl,
        inBookshelf = false,
        chapterChanged = false,
        openRequestId = 0L,
        viewModel = mangaViewModel,
        restoreSystemBarsVisible = appUi.appShell.showStatusBar,
        onFinish = { onClose() },
        onOpenBookInfo = { _, _, _ -> },
        onOpenSourceLogin = {},
        onOpenSourceEdit = {},
        onOpenWebView = { _, _, _, _, _ -> },
    )
}
