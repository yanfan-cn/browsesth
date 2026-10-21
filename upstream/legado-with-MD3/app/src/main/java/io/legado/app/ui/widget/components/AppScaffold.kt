package io.legado.app.ui.widget.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import io.legado.app.domain.model.settings.hasBackgroundImage
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LocalAppUiConfiguration
import io.legado.app.ui.theme.LocalHazeState
import io.legado.app.ui.theme.LocalTopBarBackdrop
import io.legado.app.ui.theme.responsiveHazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (HazeState) -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    contentColor: Color = contentColorFor(LegadoTheme.colorScheme.background),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    alwaysDrawBehindBars: Boolean = false,
    /**
     * 内容不作为模糊 / 液态玻璃的采样源。
     *
     * haze 的 `hazeSource` 与液态玻璃的 `layerBackdrop` 都会把整棵内容录进 GraphicsLayer；
     * 内容里若有 AndroidView（WebView 等 interop view），Compose 会把它一并画进那张离屏
     * RenderNode（`AndroidViewHolder.draw` → `AndroidComposeView.drawAndroidView`）。
     * Chromium 在「把网页画进别人的图层」这条路径上不稳定，部分设备会表现为网页闪烁，
     * 因此 WebView 类页面必须把两个采样源一起关掉。
     */
    disableContentSampling: Boolean = false,
    /**
     * SPEC-079: 本页完全不用液态玻璃 backdrop（标签页等）。
     * 为 true 时不提供 LocalTopBarBackdrop、不挂 layerBackdrop。
     */
    disableLiquidGlass: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    val isDark = LegadoTheme.isDark
    val configuration = LocalAppUiConfiguration.current
    val themeSettings = configuration.theme
    val hasImageBg = themeSettings.hasBackgroundImage(isDark)
    val hazeState = remember { HazeState() }
    // SPEC-061: liquid top-bar chrome follows global surface style.
    val liquidGlassEnabled =
        !disableLiquidGlass &&
            io.legado.app.ui.theme.ChromeSurfaceStylePolicy.isLiquidGlassActive(
                configuration.theme.chromeSurfaceStyle,
            )
    val contentDrawsBehindBars =
        alwaysDrawBehindBars || themeSettings.enableBlur || themeSettings.enableProgressiveBlur

    val containerColor = if (hasImageBg) {
        Color.Transparent
    } else {
        LegadoTheme.colorScheme.background
    }

    val topBarBackdropBaseColor = LegadoTheme.colorScheme.background
    val topBarBackgroundBackdrop = rememberLayerBackdrop {
        drawRect(topBarBackdropBaseColor)
        drawContent()
    }
    val topBarContentBackdrop = rememberLayerBackdrop { drawContent() }
    val topBarBackdrop = rememberCombinedBackdrop(
        topBarBackgroundBackdrop,
        topBarContentBackdrop
    )

    CompositionLocalProvider(
        LocalHazeState provides if (themeSettings.enableBlur) hazeState else null,
        LocalTopBarBackdrop provides if (liquidGlassEnabled) topBarBackdrop else null,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (liquidGlassEnabled) {
                                    Modifier.layerBackdrop(topBarBackgroundBackdrop)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        BackgroundImageContent(isDark = isDark, hazeState = hazeState)
                    }
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            topBar(hazeState)
                        },
                        bottomBar = bottomBar,
                        snackbarHost = snackbarHost,
                        floatingActionButton = floatingActionButton,
                        floatingActionButtonPosition = floatingActionButtonPosition,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        contentWindowInsets = contentWindowInsets
                    ) { paddingValues ->
                        val useFloating = configuration.appShell.useFloatingBottomBar
                        val scaffoldPadding = if (useFloating) {
                            PaddingValues(top = paddingValues.calculateTopPadding())
                        } else {
                            paddingValues
                        }
                        // SPEC-147 R11: consumeWindowInsets(PaddingValues) 的值随顶栏/底栏
                        // 高度变化；nav3 场景切换用 movable content 重锚页面时，attach 期
                        // update 触发 InsetsConsumingModifierNode 遍历未挂载子孙 → 上游
                        // 断言崩溃（visitSubtreeIf called on an unattached node）。
                        // 改为消费稳定的系统栏 insets，值在切页/重锚瞬间恒定 → update 不
                        // 触发 → 崩溃路径拆除。（WindowInsets.systemBars 为 @Composable
                        // 读取，须在组合期直接求值。）
                        //
                        // SPEC-148 R12（1.5.2-beta 实机回归修复）：R11 的「顶栏高度≥状态栏
                        // 高度 → 子级 inset 补零结果相同」等价性只对**有 topBar 的**
                        // AppScaffold 成立。MainScreen 外层包裹（无 topBar、contentWindow
                        // Insets = 0）旧实现消费 0，其内容区子级（NTP chrome 行
                        // BrowserNtpScreen.statusBarsPadding、平板 chrome TabletBrowser
                        // Chrome、平板设置 TabletSettingsScreen）依赖剩余状态栏 inset；
                        // R11 无条件消费 systemBars.only(Top) 使它们全部上移一个状态栏
                        // 高度、与状态栏重叠。故对 Top 侧按「scaffold 顶部 padding 是否
                        // 覆盖状态栏高度」门控：无 topBar（padding 0）时消费 0（还原旧
                        // 布局）；有 topBar 时折叠过程高度在 ≥ 状态栏区间内变化，门控
                        // 判定结果不变 → 消费值仍恒定 → R11 防崩溃保持成立。
                        // （topBar 被整体移出组合的极端场景下门控会翻转一次，此时退化为
                        // 与旧实现等价的可变消费，风险与 R11 之前相同。）
                        val systemBarsPaddingValues = WindowInsets.systemBars.asPaddingValues()
                        val topCoveredByScaffold =
                            scaffoldPadding.calculateTopPadding() >=
                                systemBarsPaddingValues.calculateTopPadding()
                        val scaffoldConsumedInsets = if (useFloating) {
                            if (topCoveredByScaffold) {
                                WindowInsets.systemBars.only(WindowInsetsSides.Top)
                            } else {
                                WindowInsets(0)
                            }
                        } else {
                            if (topCoveredByScaffold) {
                                WindowInsets.systemBars.only(
                                    WindowInsetsSides.Horizontal +
                                        WindowInsetsSides.Top +
                                        WindowInsetsSides.Bottom,
                                )
                            } else {
                                WindowInsets.systemBars.only(
                                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (liquidGlassEnabled && !disableContentSampling) {
                                        Modifier.layerBackdrop(topBarContentBackdrop)
                                    } else {
                                        Modifier
                                    }
                                )
                                .then(
                                    if (!disableContentSampling) Modifier.responsiveHazeSource(
                                        hazeState
                                    )
                                    else Modifier
                                )
                                .then(
                                    if (contentDrawsBehindBars) Modifier
                                    else Modifier
                                        .padding(scaffoldPadding)
                                        .consumeWindowInsets(scaffoldConsumedInsets)
                                )
                        ) {
                            content(
                                if (contentDrawsBehindBars) scaffoldPadding
                                else PaddingValues(0.dp)
                            )
                        }
                    }
        }
    }
}


@Composable
private fun BackgroundImageContent(
    isDark: Boolean,
    hazeState: HazeState
) {
    val themeSettings = LocalAppUiConfiguration.current.theme
    val hasImageBg = themeSettings.hasBackgroundImage(isDark)
    val bgImagePath = if (isDark) {
        themeSettings.backgroundImageDark
    } else {
        themeSettings.backgroundImageLight
    }
    val blur = if (isDark) {
        themeSettings.backgroundImageDarkBlurring
    } else {
        themeSettings.backgroundImageBlurring
    }

    if (hasImageBg && !bgImagePath.isNullOrBlank()) {
        if (themeSettings.enableBlur) {
            AsyncImage(
                model = bgImagePath,
                contentDescription = null,
                imageLoader = org.koin.compose.koinInject(),
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = bgImagePath,
                contentDescription = null,
                imageLoader = org.koin.compose.koinInject(),
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blur.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}
