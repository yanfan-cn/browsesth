package io.legado.app.ui.widget.components.icon

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import io.legado.app.R
import kotlin.math.min

data class MaterialSymbol(
    val ligature: String,
    val autoMirror: Boolean = false,
)

object MaterialSymbolAxes {
    /** SPEC-038 / UI 设计文档：Weight 400 */
    const val WEIGHT = 400
    const val GRADE = 0f
    const val OPTICAL_SIZE = 24f
}

private val MIRROR_PAIRS = mapOf(
    "arrow_back_ios_new" to "arrow_forward_ios",
    "arrow_forward_ios" to "arrow_back_ios_new",
    "arrow_back" to "arrow_forward",
    "arrow_forward" to "arrow_back",
    "arrow_left" to "arrow_right",
    "arrow_right" to "arrow_left",
    "chevron_right" to "chevron_left",
    "keyboard_arrow_left" to "keyboard_arrow_right",
    "keyboard_arrow_right" to "keyboard_arrow_left",
)

@Composable
fun SymbolIcon(
    symbol: MaterialSymbol,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    size: Dp = 24.dp,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val ligature = if (symbol.autoMirror && rtl) {
        MIRROR_PAIRS[symbol.ligature] ?: symbol.ligature
    } else {
        symbol.ligature
    }
    val context = LocalContext.current
    val typeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.material_symbols_outlined) ?: Typeface.DEFAULT
    }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val semanticsMod = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier.semantics { role = Role.Image }
    }
    BoxWithConstraints(
        modifier = modifier.then(Modifier.size(size)).then(semanticsMod),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val textSizePx = min(this.size.width, this.size.height)
            if (textSizePx <= 0f) return@Canvas
            paint.reset()
            paint.isAntiAlias = true
            paint.color = tint.toArgb()
            paint.typeface = typeface
            paint.textSize = textSizePx
            paint.textAlign = Paint.Align.CENTER
            paint.style = Paint.Style.FILL
            paint.fontVariationSettings =
                "'wght' ${MaterialSymbolAxes.WEIGHT}, " +
                    "'GRAD' ${MaterialSymbolAxes.GRADE}, " +
                    "'opsz' ${MaterialSymbolAxes.OPTICAL_SIZE}, " +
                    "'FILL' ${if (filled) 1f else 0f}"
            val metrics = paint.fontMetrics
            val baseline = this.size.height / 2f - (metrics.ascent + metrics.descent) / 2f
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    ligature,
                    this.size.width / 2f,
                    baseline,
                    paint,
                )
            }
        }
    }
}

@Composable
fun Icon(
    symbol: MaterialSymbol,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    size: Dp = 24.dp,
) {
    SymbolIcon(
        symbol = symbol,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        filled = filled,
        size = size,
    )
}
