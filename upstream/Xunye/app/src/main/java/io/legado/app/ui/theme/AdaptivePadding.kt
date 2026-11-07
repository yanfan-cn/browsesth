package io.legado.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.legado.app.ui.theme.LegadoTheme.composeEngine

@Composable
fun Modifier.adaptiveHorizontalPadding(): Modifier {
    val horizontal = 16.dp
    return this.padding(horizontal = horizontal)
}

@Composable
fun Modifier.adaptiveHorizontalPaddingTab(): Modifier {
    val start = 0.dp
    val end = 16.dp
    return this.padding(start = start, end = end)
}

@Composable
fun Modifier.adaptiveHorizontalPadding(
    vertical: Dp,
): Modifier {
    val horizontal = 16.dp
    return this.padding(horizontal = horizontal, vertical = vertical)
}

@Composable
fun Modifier.adaptiveVerticalPadding(): Modifier {
    val horizontal = 8.dp
    return this.padding(horizontal = horizontal)
}

@Composable
fun adaptiveHorizonalPadding(): PaddingValues {
    val horizontal = 16.dp
    return PaddingValues(
        horizontal = horizontal
    )
}

@Composable
fun adaptiveContentPaddingOnlyVertical(
    top: Dp,
    bottom: Dp
): PaddingValues {
    val adjustedTop = top
    return PaddingValues(
        top = adjustedTop,
        bottom = bottom,
        start = 0.dp,
        end = 0.dp
    )
}

@Composable
fun adaptiveContentPadding(
    top: Dp,
    bottom: Dp
): PaddingValues {
    val horizontal = 16.dp
    val adjustedTop = top + 16.dp
    return PaddingValues(
        top = adjustedTop,
        bottom = bottom,
        start = horizontal,
        end = horizontal
    )
}

@Composable
fun adaptiveContentPadding(
    top: Dp,
    bottom: Dp,
    miuixHorizontal: Dp,
    m3Horizontal: Dp
): PaddingValues {
    val horizontal =
        m3Horizontal
    val adjustedTop = top + 16.dp
    return PaddingValues(
        top = adjustedTop,
        bottom = bottom,
        start = horizontal,
        end = horizontal
    )
}

@Composable
fun adaptiveContentPadding(
    top: Dp,
    bottom: Dp,
    horizontal: Dp
): PaddingValues {
    val adjustedTop = top + 16.dp
    return PaddingValues(
        top = adjustedTop,
        bottom = bottom,
        start = horizontal,
        end = horizontal
    )
}

@Composable
fun adaptiveContentPaddingBookshelf(
    top: Dp,
    bottom: Dp,
    horizontal: Dp
): PaddingValues {
    val adjustedTop = top + 8.dp
    val horizontal =
        4.dp + horizontal
    return PaddingValues(
        top = adjustedTop,
        bottom = bottom,
        start = horizontal,
        end = horizontal
    )
}
