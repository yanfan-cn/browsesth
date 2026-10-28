package io.legado.app.ui.main

import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/** Matches MainNavGraph's overlay slide duration (private there). */
private const val SlideOverlayMs = 280

/**
 * SPEC-139/142 slide-only overlay. SPEC-159: stacked overlays (settings root under
 * a settings child) must not leave the covered page blank when the child pops.
 */
class SlideOverlaySceneStrategy(
    private val stillInBackStack: (Any) -> Boolean,
) : SceneStrategy<NavKey> {

    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        val entry = entries.lastOrNull() ?: return null
        entry.metadata[MetadataKey] ?: return null
        val previousEntries = entries.dropLast(1)
        if (previousEntries.isEmpty()) return null
        return SlideOverlayScene(entry, previousEntries, stillInBackStack)
    }

    companion object {
        private object MetadataKey : NavMetadataKey<Unit>
        fun slideOverlay(onBack: (() -> Unit)? = null): Map<String, Any> = metadata {
            put(MetadataKey, Unit)
            if (onBack != null) put(SlideOverlayBackKey, onBack)
        }
    }
}

internal fun slideOverlayContentKey(route: NavKey): String = "$route:${route::class}"

internal fun slideOverlayStillInStack(contentKey: Any, backStack: List<NavKey>): Boolean {
    val keyText = contentKey.toString()
    return backStack.any { route ->
        contentKey == route ||
            contentKey == slideOverlayContentKey(route) ||
            keyText == slideOverlayContentKey(route) ||
            keyText == route.toString()
    }
}

internal fun clearSlideOverlayState(route: NavKey) {
    val key = slideOverlayContentKey(route)
    SlideOverlayStates.remove(key)
    SlideOverlayStates.remove(route)
}

internal object SlideOverlayBackKey : NavMetadataKey<() -> Unit>

/**
 * Uses boolean [contentVisible] (not MutableTransitionState) so a page covered by
 * a child can remount visible without relying on a reused TransitionState that
 * may already be at "shown" and skip drawing.
 */
private class SlideOverlayState {
    var contentVisible by mutableStateOf(false)
    val gesture = Animatable(0f)

    suspend fun hideAndAwait() {
        contentVisible = false
        delay(SlideOverlayMs.toLong())
    }
}

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
    private val stillInBackStack: (Any) -> Boolean,
) : OverlayScene<NavKey> {
    override val key: Any = entry.contentKey
    override val entries: List<NavEntry<NavKey>> = listOf(entry)
    override val overlaidEntries: List<NavEntry<NavKey>> = previousEntries.takeLast(1)

    override suspend fun onRemove() {
        val state = SlideOverlayStates.get(entry.contentKey) ?: return
        val still = stillInBackStack(entry.contentKey)
        Log.i(
            "Diag159",
            "onRemove key=${entry.contentKey} stillInStack=$still " +
                "visible=${state.contentVisible} gesture=${state.gesture.value}",
        )
        // Covered by a child overlay: keep cache, stay marked visible for remount.
        if (still) return
        if (state.gesture.value > 0.99f) {
            SlideOverlayStates.remove(entry.contentKey)
            return
        }
        state.hideAndAwait()
        SlideOverlayStates.remove(entry.contentKey)
    }

    override val content: @Composable () -> Unit = {
        val state = SlideOverlayStates.get(entry.contentKey)
            ?: SlideOverlayStates.create(entry.contentKey)
        val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
        // Enter composition as top overlay → show (covers first push and child-pop remount).
        LaunchedEffect(entry.contentKey, lifecycleState) {
            if (!lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) return@LaunchedEffect
            if (state.gesture.value > 0.01f) {
                state.gesture.snapTo(0f)
            }
            if (!state.contentVisible) {
                Log.i("Diag159", "show key=${entry.contentKey}")
                state.contentVisible = true
            }
        }
        val backCallback = entry.metadata[SlideOverlayBackKey] as? (() -> Unit)
        PredictiveBackHandler(
            enabled = backCallback != null &&
                lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
        ) { events ->
            try {
                events.collect { event ->
                    state.gesture.snapTo(event.progress.coerceIn(0f, 1f))
                }
                state.gesture.animateTo(1f, tween(durationMillis = 120, easing = FastOutSlowInEasing))
                MainNavigator.resetBackNavigationGuard()
                backCallback?.invoke()
            } catch (abort: CancellationException) {
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
                visible = state.contentVisible,
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
