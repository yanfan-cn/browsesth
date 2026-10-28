package io.legado.app.ui.book.read

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.legado.app.base.BaseComposeActivity
import io.legado.app.model.ReadAloudSessionStore
import io.legado.app.model.ReadBook
import io.legado.app.service.BaseReadAloudService
import io.legado.app.ui.book.read.page.entities.PageDirection
import io.legado.app.ui.main.MainActivity
import io.legado.app.ui.main.MainIntent
import io.legado.app.ui.main.MainRouteConst
import io.legado.app.ui.widget.components.privacy.PrivateReadGate
import io.legado.app.utils.startActivity
import io.legado.app.utils.toggleSystemBar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * SPEC-190: 独立 Activity 宿主的小说阅读器（照原版 legado 的 Activity 架构）。
 *
 * 阅读器不再压在 MainActivity 的 nav3 栈上：
 * - 进入/退出转场交给系统窗口动画，MainActivity（网页/阅读列表/详情）作为底页
 *   全程保持渲染、不重组——彻底消除「退出闪切」。
 * - 阅读器内的二级目的地（搜索正文/听书投播/TTS 引擎/TTS 缓存）经
 *   `MainActivity.createXxxIntent` 以独立实例承载（与原版 BookInfoActivity 同一套模式）。
 *
 * 本 Activity 只负责 Android 层：窗口/系统栏、按键与滚轮输入、生命周期驱动的
 * resume/pause、私密阅读闸门。阅读界面与业务状态在 [ReadBookRouteScreen] 与
 * [ReadBookController]（Controller 注释明确支持任意 Activity 宿主）。
 */
class ReadBookActivity : BaseComposeActivity(imageBg = false) {

    private fun intentBookUrl(): String? = intent.getStringExtra(MainIntent.EXTRA_BOOK_URL)

    private fun intentReadAloud(): Boolean =
        intent.getBooleanExtra(MainIntent.EXTRA_READ_ALOUD, false)

    private fun intentInBookshelf(): Boolean =
        intent.getBooleanExtra(MainIntent.EXTRA_IN_BOOKSHELF, true)

    private fun intentChapterChanged(): Boolean =
        intent.getBooleanExtra(MainIntent.EXTRA_CHAPTER_CHANGED, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleSystemBar(false)
    }

    override fun onResume() {
        super.onResume()
        io.legado.app.ui.main.ReaderForegroundTracker.readerActivityInForeground = true
    }

    override fun onPause() {
        io.legado.app.ui.main.ReaderForegroundTracker.readerActivityInForeground = false
        super.onPause()
    }

    @Composable
    override fun Content() {
        val bookUrl = intentBookUrl()
        val readAloud = intentReadAloud()

        // 私密闸门：阅读记录 / 通知栏 / 深链等直开阅读器的路径全在这里被拦一次，
        // 未授权时阅读器根本不会被组合（也就没有加载正文的机会）
        PrivateReadGate(
            bookUrl = bookUrl,
            onExit = { finish() },
            onOpenLocalPasswordSettings = {
                // SPEC-192: 设置族在独立 SettingsActivity。
                startActivity(MainIntent.createSettingsIntent(this, MainRouteConst.ROUTE_SETTINGS_PRIVATE))
            },
        ) {
            val readBookViewModel = koinViewModel<ReadBookViewModel>()
            val readerSessionViewModel = koinViewModel<ReaderSessionViewModel>()
            val controller = remember(readBookViewModel, readerSessionViewModel) {
                ReadBookController(
                    this@ReadBookActivity,
                    readBookViewModel,
                    readerSessionViewModel,
                )
            }
            val lifecycleOwner = LocalLifecycleOwner.current
            val initRequest = remember(
                intentBookUrl(),
                intentInBookshelf(),
                intentChapterChanged(),
            ) {
                ReadBookInitRequest(
                    bookUrl = intentBookUrl(),
                    inBookshelf = intentInBookshelf(),
                    chapterChanged = intentChapterChanged(),
                )
            }
            val effectsReady = remember(readBookViewModel) { CompletableDeferred<Unit>() }
            val readerResumeState = remember(controller) { booleanArrayOf(false) }
            val collectorReady = remember(readBookViewModel) { booleanArrayOf(false) }
            // 是否跟随朗读位置：用户手动翻页/跳章后为 false，此时回阅读界面不回拉可见页。
            val readAloudSessionStore: ReadAloudSessionStore = koinInject()
            val readAloudFollow = remember(readBookViewModel) { booleanArrayOf(true) }
            LaunchedEffect(readAloudSessionStore) {
                readAloudSessionStore.state
                    .map { it.followReadAloudPosition }
                    .distinctUntilChanged()
                    .collect { readAloudFollow[0] = it }
            }

            fun resumeReader() {
                if (readerResumeState[0]) return
                readerResumeState[0] = true
                controller.onResume()
                readBookViewModel.onIntent(ReadBookIntent.OnResume)
                // 回到阅读界面时把可见页对齐到当前朗读位置：朗读期间用户可能进听书页跳段/跳章，
                // 服务驱动的是内部页游标，可见页需要显式回位，否则停在离开时那一页。
                if (BaseReadAloudService.isRun && readAloudFollow[0]) {
                    readBookViewModel.onIntent(ReadBookIntent.BackToSpeakingPosition)
                }
            }

            fun pauseReader() {
                if (!readerResumeState[0]) return
                readerResumeState[0] = false
                readBookViewModel.onIntent(ReadBookIntent.OnPause)
                controller.onPause()
            }

            ReadBookRouteScreen(
                viewModel = readBookViewModel,
                readerSessionViewModel = readerSessionViewModel,
                host = controller,
                controller = controller,
                onEffectsReady = { effectsReady.complete(Unit) },
                onOpenSearch = { word, searchBookUrl, autoFocus ->
                    startActivity(
                        MainIntent.createSearchContentIntent(
                            this,
                            bookUrl = searchBookUrl,
                            searchWord = word,
                            searchResultIndex = readBookViewModel.uiState.value.searchResultIndex,
                            autoFocus = autoFocus,
                        )
                    )
                },
                onOpenVoiceCasting = { voiceBookUrl ->
                    startActivity(MainIntent.createBookVoiceCastingIntent(this, voiceBookUrl))
                },
                onOpenTtsEnginesAndVoices = {
                    startActivity(MainIntent.createCloudTtsEnginesIntent(this, bookUrl))
                },
                onOpenTtsCache = {
                    startActivity(MainIntent.createTtsCacheIntent(this))
                },
            )

            DisposableEffect(controller, lifecycleOwner, readAloud) {
                activeInputHandler = controller
                controller.onStartContentLoadFinish = {
                    if (readAloud) {
                        ReadBook.readAloud()
                    }
                }

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
                    if (activeInputHandler === controller) {
                        activeInputHandler = null
                    }
                    controller.clearTts()
                    toggleSystemBar(true)
                }
            }

            LaunchedEffect(readBookViewModel, lifecycleOwner) {
                // Resolving the book and applying its read style do not depend on launcher effects.
                // Start that I/O immediately; initData still waits below because it can emit effects.
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            toggleSystemBar(true)
        }
        super.onDestroy()
    }

    // ── 按键与滚轮（原 MainActivity 阅读分支的等价实现） ──

    private var activeInputHandler: ReadBookInputHandler? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN
        if (keyCode == KeyEvent.KEYCODE_MENU && isDown) {
            activeInputHandler?.toggleMenu()
            if (activeInputHandler != null) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val handler = activeInputHandler ?: return super.onGenericMotionEvent(event)
        if (0 != (event.source and InputDevice.SOURCE_CLASS_POINTER) &&
            event.action == MotionEvent.ACTION_SCROLL
        ) {
            val axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            Log.d("onGenericMotionEvent", "axisValue = $axisValue")
            handler.mouseWheelPage(
                if (axisValue < 0.0f) PageDirection.NEXT else PageDirection.PREV
            )
            return true
        }
        if (0 != (event.source and InputDevice.SOURCE_CLASS_JOYSTICK) &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            val yAxis = event.getAxisValue(MotionEvent.AXIS_Y)
            if (kotlin.math.abs(yAxis) > 0.5f) {
                handler.handleKeyPage(
                    if (yAxis > 0) PageDirection.NEXT else PageDirection.PREV
                )
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (activeInputHandler?.onKeyDown(keyCode, event) == true) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (activeInputHandler?.onKeyUp(keyCode, event) == true) return true
        return super.onKeyUp(keyCode, event)
    }
}
