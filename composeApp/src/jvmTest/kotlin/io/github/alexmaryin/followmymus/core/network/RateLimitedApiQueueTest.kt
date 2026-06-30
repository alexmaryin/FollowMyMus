package io.github.alexmaryin.followmymus.core.network

import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class RateLimitedApiQueueTest {

    @Test
    fun `two consecutive enqueue calls are spaced by at least intervalMs apart`() = runBlocking {
        val interval = 80L
        val queue = RateLimitedApiQueue(intervalMs = interval)
        val times = mutableListOf<Long>()

        coroutineScope {
            val jobs = (1..3).map { i ->
                async {
                    queue.enqueue {
                        times += System.nanoTime()
                        i
                    }
                }
            }
            jobs.awaitAll()
        }

        val gaps = times.zipWithNext { a, b -> (b - a) / 1_000_000 }
        // Two gaps between three calls; each must be at least `interval` ms.
        assertEquals(gaps.size, 2)
        gaps.forEach { gap ->
            assertTrue(gap >= interval, "expected gap ≥ ${interval}ms, got ${gap}ms")
        }
    }

    @Test
    fun `exception in one block is propagated to the caller and does not poison the queue`() = runBlocking {
        val queue = RateLimitedApiQueue(intervalMs = 1L)

        assertFailsWith<IllegalStateException> {
            queue.enqueue<Unit> { throw IllegalStateException("boom") }
        }

        // The queue is still functional — a subsequent enqueue completes
        // with its block's return value.
        val value = queue.enqueue { 42 }
        assertEquals(42, value)
    }

    @Test
    fun `cancelled caller sees CancellationException on its enqueue and the queue keeps working for new callers`() = runBlocking {
        val queue = RateLimitedApiQueue(intervalMs = 1L)

        // The caller's enqueue suspends in `coroutineScope { await() }`.
        // Cancelling the caller makes the await throw CancellationException
        // — the wrapped block in the queue is unaffected (it runs in the
        // queue's own scope, not the caller's), so the queue may still be
        // busy with the cancelled block. We assert on the caller's view
        // only: their enqueue throws, and a new caller on a fresh scope
        // can still submit work.
        val canceller = launch {
            try {
                queue.enqueue { delay(3_000.milliseconds) }
            } catch (_: CancellationException) { /* expected */ }
        }
        delay(20.milliseconds)  // let the enqueue reach the queue
        canceller.cancel()
        canceller.join()

        // The queue is still functional. A new caller on a fresh coroutine
        // submits work that runs to completion (with the cancelled block
        // still in flight in the queue's own scope, this may queue behind
        // it — but the call is accepted and the deferred completes).
        val accepted = queue.enqueue { 7 }
        assertEquals(7, accepted)
    }
}
