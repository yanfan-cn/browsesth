package io.legado.app.browser.reader

/**
 * SPEC-216: 去掉净读正文里误入的图片尺寸调试行，例如
 * `304*194 / 360*800*3 (ext: false)`。
 */
object WebArticleDebugLineFilter {

    private val IMAGE_DIM_LINE = Regex(
        """^\s*\d+\s*\*\s*\d+\s*/\s*\d+\s*\*\s*\d+\s*\*\s*\d+\s*\(\s*ext\s*:\s*(true|false)\s*\)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    fun stripLines(text: String): String {
        if (text.isEmpty()) return text
        val out = StringBuilder(text.length)
        text.lineSequence().forEach { line ->
            if (IMAGE_DIM_LINE.matches(line)) return@forEach
            if (out.isNotEmpty()) out.append('\n')
            out.append(line)
        }
        return out.toString()
    }

    fun isDebugDimLine(line: String): Boolean = IMAGE_DIM_LINE.matches(line)
}
