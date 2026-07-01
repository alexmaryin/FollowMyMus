package io.github.alexmaryin.followmymus.core.network

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.milliseconds

/**
 * Serializes MusicBrainz API calls with a 1-second delay between each.
 *
 * MusicBrainz enforces a 1 req/s source-IP limit with a documented 100%-503
 * response on overflow (NOT a 429-with-Retry-After). The Ktor `HttpRequestRetry`
 * plugin absorbs transient 503s but does NOT space requests out — without this
 * queue, a tight sync loop locks the user out within seconds.
 *
 * The queue is a single-consumer coroutine reading from an unlimited
 * [Channel] and applying [intervalMs] between invocations. A failure inside
 * one block is caught and surfaced to the caller via [CompletableDeferred];
 * the queue itself never blocks on a single bad call.
 *
 * @param intervalMs spacing between blocks. Defaults to 1000 ms (1 req/s).
 *        Exposed as a constructor parameter so tests can use a smaller value.
 */
@Single
class RateLimitedApiQueue(
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (block in queue) {
                runCatching { block() }
                delay(intervalMs.milliseconds)
            }
        }
    }

    /**
     * Submit [block] to the queue and suspend until it has been run.
     *
     * The block runs in the queue's own scope, NOT the caller's. To make a
     * long-running block cancellable from the caller, check
     * `currentCoroutineContext().ensureActive()` (or call other suspending
     * functions) inside [block].
     *
     * Throws the same exception the block threw (via [Result.fold] semantics
     * on [CompletableDeferred.completeWith]).
     */
    fun cancel() {
        scope.cancel()
    }

    suspend fun <T> enqueue(block: suspend () -> T): T = coroutineScope {
        val deferred = CompletableDeferred<T>()
        val wrapped: suspend () -> Unit = {
            deferred.completeWith(runCatching { block() })
        }
        queue.send(wrapped)
        deferred.await()
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 1000L
    }
}
