package io.github.alexmaryin.followmymus.core.paging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Total-count holder for a network-backed paging source.
 *
 * The `PagingSource` calls [update] with the API's `count` field after
 * each successful page load. The repository exposes [flow] as its
 * `totalCount` so the UI can display "1234 results" alongside the list.
 *
 * Reset to `null` when a new search begins (so a stale count from a
 * previous query does not leak into the new one).
 */
class NetworkPagingCount {
    private val _count = MutableStateFlow<Int?>(null)

    val flow: Flow<Int?>
        get() = _count.asStateFlow()

    fun update(count: Int?) {
        _count.value = count
    }
}

/**
 * Total-count holder for a Room-backed paging source.
 *
 * The repository supplies a [countSource] lambda that returns the
 * `Flow<Int>` produced by a `SELECT COUNT(*)` query. The wrapper
 * exposes it as `Flow<Int?>` so its shape matches [NetworkPagingCount.flow]
 * and callers can `.combine` it with the paging flow uniformly.
 */
class RoomPagingCount(
    private val countSource: () -> Flow<Int>
) {
    val flow: Flow<Int?>
        get() = countSource()
}
