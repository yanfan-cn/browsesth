package io.legado.app.help.coil

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MangaPrefetchRequestsTest {
    @Test
    fun `immediate cancellation can finish all active jobs without starting pending work`() =
        runTest {
            val queue = MangaPrefetchRequests(
                CoroutineScope(
                    coroutineContext + UnconfinedTestDispatcher(testScheduler)
                )
            )
            var starts = 0
            var closes = 0
            queue.update((0..2).map { "p$it" to 0 }) {
                starts++
                try {
                    awaitCancellation()
                } finally {
                    closes++
                }
            }
            // SPEC-014: 后台任务由 2 提升到 4，3 个键全部立即启动
            assertEquals(3, starts)
            queue.close()
            assertEquals(3, closes)
            assertEquals(3, starts)
        }

    @Test
    fun `whole chapter advances with four background tasks and viewport reprioritizes only pending pages`() =
        runTest {
            val queue = MangaPrefetchRequests(this)
            val keys = (0..5).map { "p$it" to 0 }
            val started = mutableListOf<Pair<String, Int>>()
            val gates = keys.associateWith { CompletableDeferred<Unit>() }
            val prepare: suspend (Pair<String, Int>) -> Unit =
                { key -> started += key; gates.getValue(key).await() }
            queue.update(keys, prepare)
            runCurrent()
            // SPEC-014: 并发 4 → 首轮启动前 4 页
            assertEquals(keys.take(4), started)
            queue.update(keys.reversed(), prepare)
            runCurrent()
            // 活跃任务不被视口重排打断，只有 pending 重排
            assertEquals(keys.take(4), started)
            gates.getValue(keys[0]).complete(Unit)
            runCurrent()
            // k0 完成腾出槽位；reversed 后 wanted=[k5,k4,k3,k2,k1,k0]，pending 首个是 k5
            assertEquals(listOf(keys[0], keys[1], keys[2], keys[3], keys[5]), started)
            gates.values.forEach { it.complete(Unit) }
            runCurrent()
            // 剩余 pending 只有 k4，在下一个完成回调里启动
            assertEquals(
                listOf(keys[0], keys[1], keys[2], keys[3], keys[5], keys[4]),
                started,
            )
            queue.update(keys, prepare)
            runCurrent()
            assertEquals(6, started.size)
            queue.close()
        }

    @Test
    fun `chapter exit cancels active work and failure does not block later pages or spin`() =
        runTest {
            val queue = MangaPrefetchRequests(this)
            val cancelled = mutableListOf<String>()
            queue.update(listOf("old1" to 0, "old2" to 0)) { key ->
                try {
                    awaitCancellation()
                } finally {
                    cancelled += key.first
                }
            }
            runCurrent()
            val started = mutableListOf<Pair<String, Int>>()
            val prepare: suspend (Pair<String, Int>) -> Unit = { key ->
                started += key
                if (key.first == "failed") error("network failure")
            }
            val next = listOf("failed" to 0, "next" to 0, "last" to 0)
            queue.update(next, prepare)
            runCurrent()
            assertEquals(listOf("old1", "old2"), cancelled)
            assertEquals(next, started)
            queue.update(next, prepare)
            runCurrent()
            assertEquals(next, started)
            queue.update(listOf("failed" to 1, "next" to 0, "last" to 0), prepare)
            runCurrent()
            assertEquals(next + ("failed" to 1), started)
            queue.close()
            queue.update(listOf("unused" to 0), prepare)
            runCurrent()
            assertEquals(4, started.size)
        }

    @Test
    fun `disabling prefetch releases active work and reenabling prepares again`() = runTest {
        val queue = MangaPrefetchRequests(this)
        var starts = 0
        var closes = 0
        val prepare: suspend (Pair<String, Int>) -> Unit = {
            starts++
            try {
                awaitCancellation()
            } finally {
                closes++
            }
        }
        queue.update(listOf("page" to 0), prepare)
        runCurrent()
        queue.update(emptyList(), prepare)
        runCurrent()
        assertEquals(1, closes)
        queue.update(listOf("page" to 0), prepare)
        runCurrent()
        assertEquals(2, starts)
        queue.close()
        queue.close()
        runCurrent()
        assertEquals(2, closes)
    }
}
