package io.legado.app.ui.widget.components.swipe

import io.legado.app.ui.widget.components.icon.MaterialSymbol
import androidx.compose.ui.graphics.Color

data class SwipeAction(
    val icon: MaterialSymbol,
    val background: Color,
    val onSwipe: () -> Unit,
    val hapticFeedback: Boolean = true,
    val contentDescription: String? = null
)
