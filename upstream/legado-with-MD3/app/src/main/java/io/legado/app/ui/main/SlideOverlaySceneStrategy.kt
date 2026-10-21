package io.legado.app.ui.main

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

/** Matches MainNavGraph's overlay slide duration (private there). */
private const val SlideOverlayMs = 280

/**
 * SPEC-139: history / bookmarks / tabs render as a self-animating slide-only
 * overlay whose underlay (browser Home) stays composed underneath: both push
 * and pop leave Home untouched while the page slides in / out above it.
 *
 * Pop plays the slide-out inside [OverlayScene.onRemove]; nav3 keeps the scene
 * in composition until it finishes (NavDisplay.kt: "let onRemove finish before
 * removing from composition to ensure animations can complete"), so the live
 * underlay is revealed with zero recomposition on the animated frames.
 * No fade/scale over AwContents / liquid glass.
 *
 * SPEC-142: back gesture follows the finger. nav3's built-in predictive seek
 * only drives its main AnimatedContent, which overlay scenes are rendered
 * above, so the overlay tracks `NavigationEvent.progress` itself via
 * [PredictiveBackHandler] and translates its content with the gesture.
 * Commit finishes the slide then pops through the callback injected by
 * MainNavGraph; cancel settles the page back in place.
 */
class SlideOverlaySceneStrategy : SceneStrategy<NavKey> {

    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        val entry = entries.lastOrNull() ?: return null
        entry.metadata[MetadataKey] ?: return null
        val previousEntries = entries.dropLast(1)
        if (previousEntries.isEmpty()) return null
        return SlideOverlayScene(entry, previousEntries)
    }

    companion object {
        private object MetadataKey : NavMetadataKey<Unit>
        // SlideOverlayBackKey is a top-level object (see below).
        fun slideOverlay(onBack: (() -> Unit)? = null): Map<String, Any> = metadata {
            put(MetadataKey, Unit)
            if (onBack != null) put(SlideOverlayBackKey, onBack)
        }
    }
}

/** SPEC-142: optional back callback (injected by MainNavGraph) for gesture-driven pop. */
internal object SlideOverlayBackKey : NavMetadataKey<() -> Unit>

/** Slide visibility shared between the scene content and [SlideOverlayScene.onRemove]. */
private class SlideOverlayState {
    // Fresh push: start hidden and slide in immediately (first frame onwards).
    val visibleState = MutableTransitionState(false).apply { targetState = true }

    /** SPEC-142: 0 = at rest, 1 = fully slid out; driven by back-gesture progress. */
    val gesture = Animatable(0f)

    suspend fun hideAndAwait() {
        visibleState.targetState = false
        snapshotFlow { visibleState.isIdle && !visibleState.currentState }.first { it }
    }
}

/**
 * Persists across scene re-creation within one overlay lifetime (e.g. back
 * from the reader shows the page in place, no replayed slide); cleared when
 * the page pops for good so the next push slides in again.
 */
private object SlideOverlayStates {
    private val states = mutableMapOf<Any, SlideOverlayState>()

    fun get(key: Any): SlideOverlayState? = states[key]

    fun create(key: Any): SlideOverlayState =
        SlideOverlayState().also { states[key] = it }

    fun remove(key: Any) {
        states.remove(key)
    }
}

private data class SlideOverlayScene(
    private val entry: NavEntry<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
) : OverlayScene<NavKey> {
    override val key: Any = entry.contentKey
    override val entries: List<NavEntry<NavKey>> = listOf(entry)
    override val overlaidEntries: List<NavEntry<NavKey>> = previousEntries.takeLast(1)

    override suspend fun onRemove() {
        val state = SlideOverlayStates.get(entry.contentKey) ?: return
        // Gesture-completed pops are already fully off-screen; skip the replay.
        if (state.gesture.value > 0.99f) {
            SlideOverlayStates.remove(entry.contentKey)
            return
        }
        state.hideAndAwait()
        SlideOverlayStates.remove(entry.contentKey)
    }

    override val content: @Composable () -> Unit = {
        // Registry lookup (not remember): the scene instance is re-created on
        // every backstack change, the state must survive across those.
        val state = SlideOverlayStates.get(entry.contentKey)
            ?: SlideOverlayStates.create(entry.contentKey)
        val backCallback = entry.metadata[SlideOverlayBackKey] as? (() -> Unit)
        val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
        // SPEC-142: follow-finger back. Enabled only while this overlay is the
        // top scene (nav3 holds covered overlays at STARTED), so stacked
        // overlays always let the topmost one win the gesture.
        PredictiveBackHandler(
            enabled = backCallback != null &&
                lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
        ) { events ->
            try {
                events.collect { event ->
                    state.gesture.snapTo(event.progress.coerceIn(0f, 1f))
                }
                // Committed: finish the slide, then pop. The pop's onRemove
                // sees progress == 1 and skips its own (invisible) exit wait.
                state.gesture.animateTo(1f, tween(durationMillis = 120, easing = FastOutSlowInEasing))
                // SPEC-147 R10: 手势 commit 是明确的用户意图——复位导航防抖，
                // 避免上一页导航后 500ms 窗口内的 commit 被吞（页面已滑出屏幕
                // 却未 pop，需要滑两次才生效）。
                MainNavigator.resetBackNavigationGuard()
                backCallback?.invoke()
            } catch (abort: CancellationException) {
                // Cancelled: settle the page back in place.
                state.gesture.animateTo(0f, tween(durationMillis = 150, easing = FastOutSlowInEasing))
                throw abort
            }
        }
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = size.width * state.gesture.value
            },
        ) {
            AnimatedVisibility(
                visibleState = state.visibleState,
                enter = slideInHorizontally(
                    animationSpec = tween(durationMillis = SlideOverlayMs, easing = FastOutSlowInEasing),
                ) { fullWidth -> fullWidth },
                exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = SlideOverlayMs, easing = FastOutSlowInEasing),
                ) { fullWidth -> fullWidth },
            ) {
                entry.Content()
            }
        }
    }
}
