package io.legado.app.ui.book.readaloud.player

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 语速调节范围相关纯逻辑测试。
 *
 * 契约依据（SPEC-148 R1b，产品自 SPEC-112 起改版）：
 * `io.legado.app.help.readaloud.ReadAloudSpeechRate` —
 * 0.5x~4.0x，存储值 MIN_STORED=2 .. MAX_STORED=16（步进 0.25x，即存储值步进 1），
 * DEFAULT_STORED=4（=1.0x），sliderSteps=13；
 * toMultiplier(stored)=coerce(stored)/4f；formatLabel 整数倍 "x.0"、非整数 "%.2f"。
 * 旧上游契约（0..80 步进 1，issue #2032 时代）已废弃，测试随现行契约重写。
 */
class ReadAloudPlayerSpeedRangeTest {

    @Test
    fun `speed range is 0_5x to 4x per SPEC-112`() {
        // 存储值 2=0.5x、16=4.0x；播放器滑杆 valueRange 用 MIN/MAX 常量
        assertEquals(2, READ_ALOUD_SPEED_MIN)
        assertEquals(16, READ_ALOUD_SPEED_MAX)
    }

    @Test
    fun `slider steps cover every 0_25x increment`() {
        // 2..16 共 15 个刻度点，steps = 刻度点 - 1 = 13（0.5x→4.0x 步进 0.25x）
        assertEquals(13, READ_ALOUD_SPEED_MAX - READ_ALOUD_SPEED_MIN - 1)
    }

    @Test
    fun `coerce clamps below minimum`() {
        assertEquals(READ_ALOUD_SPEED_MIN, coerceReadAloudSpeed(-5))
        assertEquals(READ_ALOUD_SPEED_MIN, coerceReadAloudSpeed(0))
        assertEquals(READ_ALOUD_SPEED_MIN, coerceReadAloudSpeed(Int.MIN_VALUE))
    }

    @Test
    fun `coerce clamps above maximum`() {
        assertEquals(READ_ALOUD_SPEED_MAX, coerceReadAloudSpeed(999))
        assertEquals(READ_ALOUD_SPEED_MAX, coerceReadAloudSpeed(20))
        assertEquals(READ_ALOUD_SPEED_MAX, coerceReadAloudSpeed(Int.MAX_VALUE))
    }

    @Test
    fun `coerce keeps in-range values unchanged`() {
        assertEquals(2, coerceReadAloudSpeed(2))
        assertEquals(4, coerceReadAloudSpeed(4))
        assertEquals(8, coerceReadAloudSpeed(8))
        assertEquals(16, coerceReadAloudSpeed(16))
    }

    @Test
    fun `format label shows x_0 for whole multipliers`() {
        // stored 4=1.0x、8=2.0x、16=4.0x
        assertEquals("0.50", formatReadAloudSpeedLabel(2))
        assertEquals("1.0", formatReadAloudSpeedLabel(4))
        assertEquals("2.0", formatReadAloudSpeedLabel(8))
        assertEquals("3.50", formatReadAloudSpeedLabel(14))
        assertEquals("4.0", formatReadAloudSpeedLabel(16))
    }

    @Test
    fun `format label keeps two decimal places for quarter steps`() {
        // stored 5=1.25x、7=1.75x（非整数倍走 "%.2f"）
        assertEquals("1.25", formatReadAloudSpeedLabel(5))
        assertEquals("1.75", formatReadAloudSpeedLabel(7))
    }

    @Test
    fun `format label coerces out-of-range stored values`() {
        // toMultiplier 先 coerce：越界值按边界显示（回归：旧 cap 语义值不再出现）
        assertEquals("0.50", formatReadAloudSpeedLabel(0))
        assertEquals("4.0", formatReadAloudSpeedLabel(20))
        assertEquals("4.0", formatReadAloudSpeedLabel(Int.MAX_VALUE))
    }
}