package io.legado.app.browser.engine

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * SPEC-207 / SPEC-146 R1：地址栏获焦后点击落点会覆盖选区。
 * 等两帧再全选，让指针手势的 selection 更新先落盘。
 */
suspend fun reselectAllOmniboxText(
    get: () -> TextFieldValue,
    set: (TextFieldValue) -> Unit,
) {
    withFrameNanos { }
    withFrameNanos { }
    val current = get()
    val text = current.text
    if (text.isEmpty()) return
    val full = TextRange(0, text.length)
    if (current.selection == full) return
    set(current.copy(selection = full))
}
