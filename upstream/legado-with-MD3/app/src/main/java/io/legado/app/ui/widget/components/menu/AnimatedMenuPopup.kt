package io.legado.app.ui.widget.components.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * SPEC-198: Material Motion 菜单弹出容器。
 *
 * 裸 [Popup] 与 material3 [androidx.compose.material3.DropdownMenu] 在本工程版本里
 * 弹出均为瞬时出现（操作者 2026-10-28 反馈）。Material Motion 的菜单规范：
 * 淡入 ~150ms（减速）+ 从锚点角缩放 0.92→1。
 *
 * 与 DropdownMenu 的差异：定位由 [alignment] + [offset] 直接给出（不再自动翻转/
 * 夹边），调用方需按锚点在屏幕中的位置选择 [MenuPopupAlignment]：底栏锚点用
 * Above*（向上展开），顶栏锚点用 Below*（向下展开）；锚点靠左用 *Start、靠右用 *End。
 * surface（圆角/底色/阴影）由 content 自带，本组件只负责定位与动效；消失路径保持
 * 即时（与 DropdownMenu 一致）。
 *
 * SPEC-200 R2: 传 [barEdge] 且外层存在 [MenuBarBoundsProvider] 时，改走
 * [PopupPositionProvider]——菜单**整体**置于栏容器外 4dp（底栏锚点在栏上方，顶栏锚点
 * 在栏下方），水平贴锚点键（[alignEnd] = 锚右缘对菜单右缘）；栏边界缺失时回退
 * alignment+offset 旧行为。菜单尺寸在首帧为 0，Popup 会在测量后自动重定位。
 */
enum class MenuPopupAlignment(
    val alignment: Alignment,
    val transformOrigin: TransformOrigin,
) {
    AboveStart(Alignment.BottomStart, TransformOrigin(0f, 1f)),
    AboveEnd(Alignment.BottomEnd, TransformOrigin(1f, 1f)),
    BelowStart(Alignment.TopStart, TransformOrigin(0f, 0f)),
    BelowEnd(Alignment.TopEnd, TransformOrigin(1f, 0f)),
}

/** SPEC-200: 菜单相对栏容器的展开方向。 */
enum class MenuPopupBarEdge { Up, Down }

/** SPEC-200: 锚点键所在的 chrome 栏窗口边界；null = 未提供（回退锚点对齐）。 */
val LocalMenuBarBounds = staticCompositionLocalOf<Rect?> { null }

/** SPEC-200: 测量并向下提供所在栏容器的窗口边界（供菜单整体置于栏外）。 */
@Composable
fun MenuBarBoundsProvider(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }
    CompositionLocalProvider(LocalMenuBarBounds provides bounds) {
        Box(
            modifier = modifier.onGloballyPositioned { coords ->
                bounds = coords.boundsInWindow()
            },
        ) {
            content()
        }
    }
}

private const val EnterDurationMs = 150

/** SPEC-200: 菜单与栏缘的间距。 */
private val MenuBarGap = 4.dp

@Composable
fun AnimatedMenuPopup(
    alignment: MenuPopupAlignment,
    modifier: Modifier = Modifier,
    offset: IntOffset = IntOffset.Zero,
    /** SPEC-200: 指定后按 [LocalMenuBarBounds] 把菜单整体放到栏外 4dp。 */
    barEdge: MenuPopupBarEdge? = null,
    /** SPEC-200: barEdge 生效时菜单水平贴靠侧（true=锚右缘/菜单右缘）。 */
    alignEnd: Boolean = false,
    focusable: Boolean = true,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val density = LocalDensity.current
    val barBounds = LocalMenuBarBounds.current
    val barPositionProvider = remember(barEdge, alignEnd, barBounds, density) {
        if (barEdge == null) return@remember null
        val edge = barEdge
        val end = alignEnd
        val bounds: IntRect? = barBounds?.let { rect ->
            IntRect(
                rect.left.roundToInt(),
                rect.top.roundToInt(),
                rect.right.roundToInt(),
                rect.bottom.roundToInt(),
            )
        }
        val fallbackOffsetY = offset.y
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val gap = with(density) { MenuBarGap.roundToPx() }
                val maxLeft = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                val x = (if (end) {
                    anchorBounds.right - popupContentSize.width
                } else {
                    anchorBounds.left
                }).coerceIn(0, maxLeft)
                return if (bounds == null) {
                    // 回退：栏边界未知时按锚点对齐 + 调用方 offset（R1 行为）。
                    when (edge) {
                        MenuPopupBarEdge.Up ->
                            IntOffset(x, anchorBounds.bottom - popupContentSize.height + fallbackOffsetY)
                        MenuPopupBarEdge.Down -> IntOffset(x, anchorBounds.top + fallbackOffsetY)
                    }
                } else {
                    when (edge) {
                        MenuPopupBarEdge.Up -> IntOffset(
                            x,
                            (bounds.top - gap - popupContentSize.height).coerceAtLeast(0),
                        )
                        MenuPopupBarEdge.Down -> IntOffset(
                            x,
                            (bounds.bottom + gap).coerceAtMost(
                                (windowSize.height - popupContentSize.height).coerceAtLeast(0)
                            ),
                        )
                    }
                }
            }
        }
    }
    val popupContent: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(EnterDurationMs, easing = LinearOutSlowInEasing)) +
                scaleIn(
                    initialScale = 0.92f,
                    transformOrigin = alignment.transformOrigin,
                    animationSpec = tween(EnterDurationMs, easing = FastOutSlowInEasing),
                ),
            modifier = modifier,
        ) {
            Column(content = content)
        }
    }
    if (barPositionProvider != null) {
        Popup(
            popupPositionProvider = barPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = focusable),
        ) {
            popupContent()
        }
    } else {
        Popup(
            alignment = alignment.alignment,
            offset = offset,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = focusable),
        ) {
            popupContent()
        }
    }
}
