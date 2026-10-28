package io.legado.app.ui.widget.components.image.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import kotlin.math.absoluteValue

/**
 * SPEC-160/164: persistent color / gradient covers stored in customCoverUrl / coverUrl.
 *
 * Formats:
 * - `browsesth-cover://v1/solid/{AARRGGBB}`
 * - `browsesth-cover://v1/gradient/{AARRGGBB}/{AARRGGBB}`
 */
object GeneratedCover {
    const val SCHEME_PREFIX = "browsesth-cover://v1/"

    data class Solid(val color: Color)
    data class Gradient(val start: Color, val end: Color)

    sealed class Spec {
        data class SolidSpec(val solid: Solid) : Spec()
        data class GradientSpec(val gradient: Gradient) : Spec()
    }

    fun isGenerated(path: String?): Boolean =
        !path.isNullOrBlank() && path.startsWith(SCHEME_PREFIX)

    fun solidUri(argb: Long): String =
        "${SCHEME_PREFIX}solid/${argbHex(argb)}"

    fun gradientUri(startArgb: Long, endArgb: Long): String =
        "${SCHEME_PREFIX}gradient/${argbHex(startArgb)}/${argbHex(endArgb)}"

    private fun argbHex(argb: Long): String =
        (argb and 0xFFFFFFFFL).toString(16).padStart(8, '0')

    fun parse(path: String?): Spec? {
        if (!isGenerated(path)) return null
        val rest = path!!.removePrefix(SCHEME_PREFIX)
        val parts = rest.split('/')
        return when (parts.getOrNull(0)) {
            "solid" -> {
                val c = parseArgb(parts.getOrNull(1)) ?: return null
                Spec.SolidSpec(Solid(c))
            }
            "gradient" -> {
                val a = parseArgb(parts.getOrNull(1)) ?: return null
                val b = parseArgb(parts.getOrNull(2)) ?: return null
                Spec.GradientSpec(Gradient(a, b))
            }
            else -> null
        }
    }

    /**
     * SPEC-164: pick from [pool] (enabled presets + gallery). Empty pool → all built-in presets.
     * Stable for the same [seed].
     */
    fun fromSeed(seed: String, pool: List<String> = presets.map { it.uri }): String {
        val palette = pool.filter { it.isNotBlank() }.ifEmpty { presets.map { it.uri } }
        val idx = seed.hashCode().absoluteValue % palette.size
        return palette[idx]
    }

    data class Preset(
        val id: String,
        val label: String,
        val uri: String,
        val preview: Spec,
        /** SPEC-164: Material bookmark badge on the six legacy gradients. */
        val showBookmark: Boolean = false,
    )

    /** SPEC-164: 6 gradients (bookmark) + light-gray gradient + cream solid. */
    val presets: List<Preset> = listOf(
        Preset(
            id = "hai_mu",
            label = "海暮",
            uri = gradientUri(0xFF0F2027, 0xFF2C5364),
            preview = Spec.GradientSpec(Gradient(Color(0xFF0F2027), Color(0xFF2C5364))),
            showBookmark = true,
        ),
        Preset(
            id = "qiu_ye",
            label = "秋叶",
            uri = gradientUri(0xFF3E1F0A, 0xFFB36A2E),
            preview = Spec.GradientSpec(Gradient(Color(0xFF3E1F0A), Color(0xFFB36A2E))),
            showBookmark = true,
        ),
        Preset(
            id = "ye_ying",
            label = "夜樱",
            uri = gradientUri(0xFF2B1B2E, 0xFF8E3B5B),
            preview = Spec.GradientSpec(Gradient(Color(0xFF2B1B2E), Color(0xFF8E3B5B))),
            showBookmark = true,
        ),
        Preset(
            id = "zhu_ying",
            label = "竹影",
            uri = gradientUri(0xFF1A2F23, 0xFF4F7A5A),
            preview = Spec.GradientSpec(Gradient(Color(0xFF1A2F23), Color(0xFF4F7A5A))),
            showBookmark = true,
        ),
        Preset(
            id = "shuang_tian",
            label = "霜天",
            uri = gradientUri(0xFF1C2331, 0xFF5B7C99),
            preview = Spec.GradientSpec(Gradient(Color(0xFF1C2331), Color(0xFF5B7C99))),
            showBookmark = true,
        ),
        Preset(
            id = "hu_po",
            label = "琥珀",
            uri = gradientUri(0xFF2A1A0A, 0xFFC48A3A),
            preview = Spec.GradientSpec(Gradient(Color(0xFF2A1A0A), Color(0xFFC48A3A))),
            showBookmark = true,
        ),
        Preset(
            id = "qian_hui",
            label = "浅灰",
            uri = gradientUri(0xFFE8E8E8, 0xFF9E9E9E),
            preview = Spec.GradientSpec(Gradient(Color(0xFFE8E8E8), Color(0xFF9E9E9E))),
            showBookmark = false,
        ),
        Preset(
            id = "nai_you",
            label = "奶油",
            uri = solidUri(0xFFF6F1EA),
            preview = Spec.SolidSpec(Solid(Color(0xFFF6F1EA))),
            showBookmark = false,
        ),
    )

    fun presetForUri(uri: String?): Preset? =
        presets.firstOrNull { it.uri == uri }

    private fun parseArgb(hex: String?): Color? {
        if (hex.isNullOrBlank()) return null
        val cleaned = hex.removePrefix("#")
        val value = cleaned.toLongOrNull(16) ?: return null
        return Color((value and 0xFFFFFFFFL).toInt())
    }
}

@Composable
fun GeneratedCoverFace(
    path: String,
    title: String? = null,
    modifier: Modifier = Modifier,
    showBookmarkOverride: Boolean? = null,
) {
    val spec = GeneratedCover.parse(path) ?: return
    val brush = when (spec) {
        is GeneratedCover.Spec.SolidSpec -> Brush.verticalGradient(
            listOf(spec.solid.color, spec.solid.color),
        )
        is GeneratedCover.Spec.GradientSpec -> Brush.verticalGradient(
            listOf(spec.gradient.start, spec.gradient.end),
        )
    }
    val showBookmark = showBookmarkOverride
        ?: (GeneratedCover.presetForUri(path)?.showBookmark == true)
    // Cream / light gray need dark title for contrast.
    val onLight = when (spec) {
        is GeneratedCover.Spec.SolidSpec ->
            spec.solid.color.red + spec.solid.color.green + spec.solid.color.blue > 2.2f
        is GeneratedCover.Spec.GradientSpec -> {
            val a = spec.gradient.start
            val b = spec.gradient.end
            (a.red + a.green + a.blue + b.red + b.green + b.blue) / 2f > 2.0f
        }
    }
    val titleColor = if (onLight) Color(0xFF1C1B19).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.92f)
    val bookmarkTint = if (onLight) Color(0xFF1C1B19).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.9f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush),
    ) {
        if (showBookmark) {
            SymbolIcon(
                symbol = Symbols.Bookmark,
                contentDescription = null,
                tint = bookmarkTint,
                filled = true,
                size = 18.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            )
        }
        val label = title?.trim().orEmpty()
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = titleColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}
