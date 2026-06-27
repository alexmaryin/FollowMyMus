package io.github.alexmaryin.followmymus.core.ui

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.github.alexmaryin.followmymus.core.paging.PagingError
import io.github.alexmaryin.followmymus.core.paging.defaultPagingError

@DslMarker
annotation class PagingDSL

/**
 * Computed once per recomposition from `LazyPagingItems.loadState`
 * and `itemCount`. The paging wrapper dispatches on exactly one of
 * these states — there is no "fall-through" or declaration-order
 * behavior to remember.
 */
sealed interface PagingUiState<out T : Any> {
    data object Loading : PagingUiState<Nothing>
    data object Empty : PagingUiState<Nothing>
    data class Error(val error: PagingError) : PagingUiState<Nothing>
    data class Content<T : Any>(val items: LazyPagingItems<T>) : PagingUiState<T>
}

@PagingDSL
class PagingHandlerScope<T : Any>(
    private val items: LazyPagingItems<T>,
    private val state: PagingUiState<T>,
    private val errorMapper: (Throwable) -> PagingError,
) {
    private val loadState: State<CombinedLoadStates> = derivedStateOf { items.loadState }

    @Composable
    fun OnLoading(body: @Composable () -> Unit) {
        if (state is PagingUiState.Loading) body()
    }

    @Composable
    fun OnEmpty(body: @Composable () -> Unit) {
        if (state is PagingUiState.Empty) body()
    }

    @Composable
    fun OnError(body: @Composable (PagingError) -> Unit) {
        (state as? PagingUiState.Error)?.let { body(it.error) }
    }

    @Composable
    fun OnContent(body: @Composable (LazyPagingItems<T>) -> Unit) {
        if (state is PagingUiState.Content) {
            body(items)
        }
    }

    fun LazyListScope.onAppendLoading(body: @Composable LazyItemScope.() -> Unit) {
        val append = loadState.value.append
        if (append is LoadState.Loading && !append.endOfPaginationReached) {
            item { body(this) }
        }
    }

    fun LazyListScope.onAppendError(
        body: @Composable LazyItemScope.(PagingError, () -> Unit) -> Unit
    ) {
        val append = loadState.value.append
        if (append is LoadState.Error) {
            val error = errorMapper(append.error)
            val retry: () -> Unit = { items.retry() }
            item { body(this, error, retry) }
        }
    }

    fun LazyListScope.onLastItem(body: @Composable LazyItemScope.() -> Unit) {
        if (loadState.value.append.endOfPaginationReached) item { body(this) }
    }

    fun LazyListScope.onPagingItems(
        key: ((T) -> Any)?,
        contentType: (T) -> Any? = { null },
        body: @Composable LazyItemScope.(T) -> Unit,
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey(key),
            contentType = items.itemContentType(contentType),
        ) { index ->
            items[index]?.let { body(it) }
        }
    }
}

@Composable
fun <T : Any> HandlePagingItems(
    items: LazyPagingItems<T>,
    errorMapper: (Throwable) -> PagingError = ::defaultPagingError,
    onErrorAction: (PagingError) -> Unit = {},
    content: @Composable PagingHandlerScope<T>.() -> Unit,
) {
    val refresh = items.loadState.refresh
    val itemCount = items.itemCount

    val state: PagingUiState<T> = when {
        refresh is LoadState.Loading && itemCount == 0 -> PagingUiState.Loading
        refresh is LoadState.Error && itemCount == 0 -> PagingUiState.Error(errorMapper(refresh.error))
        refresh is LoadState.Error -> PagingUiState.Content(items)
        itemCount == 0 -> PagingUiState.Empty
        else -> PagingUiState.Content(items)
    }

    val refreshError = (refresh as? LoadState.Error)?.error
    LaunchedEffect(refreshError) {
        if (refresh is LoadState.Error && itemCount > 0) {
            onErrorAction(errorMapper(refresh.error))
        }
    }

    PagingHandlerScope(items, state, errorMapper).content()
}
