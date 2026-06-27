package io.github.alexmaryin.followmymus.core.paging

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map

/**
 * Result of grouping a [PagingData] stream: a flat sequence of
 * headers and items, suitable for rendering with sticky headers in
 * a `LazyColumn` / `LazyGrid`.
 */
sealed interface GroupedItem<out T, out K> {
    data class Header<K>(val key: K) : GroupedItem<Nothing, K>
    data class Item<T>(val value: T) : GroupedItem<T, Nothing>
}

/**
 * Insert a [GroupedItem.Header] whenever [keySelector] returns a
 * different key for two consecutive items, and emit every original
 * element wrapped as [GroupedItem.Item].
 *
 * **Callers should `map` and apply any other transforms to the
 * underlying `T` *before* calling `groupedBy`.** Once items are
 * wrapped in [GroupedItem], reaching the inner `T` requires a
 * `when` exhaust and a cast, and re-mapping after grouping is not
 * supported by Paging 3.
 */
fun <T : Any, K : Any> PagingData<T>.groupedBy(
    keySelector: (T) -> K
): PagingData<GroupedItem<T, K>> = this
    .map<T, GroupedItem.Item<T>> { GroupedItem.Item(it) }
    .insertSeparators<GroupedItem.Item<T>, GroupedItem<T, K>> { before, after ->
        if (after == null) return@insertSeparators null
        val afterKey = keySelector(after.value)
        val beforeKey = before?.value?.let(keySelector)
        if (beforeKey != afterKey) GroupedItem.Header(afterKey) else null
    }
