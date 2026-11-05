package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-230: 自动清理判定与容量文案的纯函数测试。 */
class StorageCacheAutoTrimTest {

    @Test
    fun `no trim when under caps`() {
        val result = StorageCacheAutoTrim.decide(
            kernelHttpBytes = 200L * 1024 * 1024,
            kernelDiagBytes = 50L * 1024 * 1024,
            kernelCapBytes = 200L * 1024 * 1024,
            metricsCapBytes = 50L * 1024 * 1024,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `trims kernel http cache over cap`() {
        val result = StorageCacheAutoTrim.decide(
            kernelHttpBytes = 201L * 1024 * 1024,
            kernelDiagBytes = 0L,
            kernelCapBytes = 200L * 1024 * 1024,
            metricsCapBytes = 50L * 1024 * 1024,
        )
        assertEquals(setOf(StorageCacheCategory.KERNEL_HTTP), result)
    }

    @Test
    fun `trims metrics over cap`() {
        val result = StorageCacheAutoTrim.decide(
            kernelHttpBytes = 0L,
            kernelDiagBytes = 51L * 1024 * 1024,
            kernelCapBytes = 200L * 1024 * 1024,
            metricsCapBytes = 50L * 1024 * 1024,
        )
        assertEquals(setOf(StorageCacheCategory.KERNEL_DIAG), result)
    }

    @Test
    fun `trims both when both over cap`() {
        val result = StorageCacheAutoTrim.decide(
            kernelHttpBytes = 300L * 1024 * 1024,
            kernelDiagBytes = 600L * 1024 * 1024,
            kernelCapBytes = 200L * 1024 * 1024,
            metricsCapBytes = 50L * 1024 * 1024,
        )
        assertEquals(
            setOf(StorageCacheCategory.KERNEL_HTTP, StorageCacheCategory.KERNEL_DIAG),
            result,
        )
    }

    @Test
    fun `zero cap disables category trim`() {
        val result = StorageCacheAutoTrim.decide(
            kernelHttpBytes = Long.MAX_VALUE,
            kernelDiagBytes = Long.MAX_VALUE,
            kernelCapBytes = 0L,
            metricsCapBytes = 0L,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `format size boundaries`() {
        assertEquals("0 MB", formatStorageSize(0L))
        assertEquals("小于 1 MB", formatStorageSize(1024L * 1024L - 1))
        assertEquals("1 MB", formatStorageSize(1024L * 1024L))
        assertEquals("220 MB", formatStorageSize(220L * 1024L * 1024L))
    }
}
