package io.legado.app.help.readaloud

import kotlin.math.roundToInt

/**
 * SPEC-112: stored speech-rate scale.
 * Stored value is quarter-steps: 2 -> 0.5x ... 16 -> 4.0x (step 0.25x).
 * Legacy 0..80 used rate = (v + 5) / 10; see [migrateFromLegacy].
 */
object ReadAloudSpeechRate {
    const val MIN_STORED = 2
    const val MAX_STORED = 16
    const val DEFAULT_STORED = 4

    fun coerce(stored: Int): Int = stored.coerceIn(MIN_STORED, MAX_STORED)

    fun toMultiplier(stored: Int): Float = coerce(stored) / 4f

    fun fromMultiplier(rate: Float): Int =
        (rate * 4f).roundToInt().coerceIn(MIN_STORED, MAX_STORED)

    /** Legacy 0..80 ((v+5)/10) -> new scale. */
    fun migrateFromLegacy(old: Int): Int {
        val legacyRate = (old + 5) / 10f
        return fromMultiplier(legacyRate)
    }

    fun formatLabel(stored: Int): String {
        val display = toMultiplier(stored)
        return if (display == display.roundToInt().toFloat()) {
            "${display.roundToInt()}.0"
        } else {
            String.format("%.2f", display)
        }
    }

    val sliderSteps: Int get() = MAX_STORED - MIN_STORED - 1
}