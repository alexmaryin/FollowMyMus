package io.github.alexmaryin.followmymus.core.ui

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchError
import io.ktor.client.call.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.util.network.*

@DslMarker
annotation class PagingDSL

@PagingDSL
class PagingHandlerScope<T : Any>(
    private val items: LazyPagingItems<T>
) {
    private var handled = false
    private val loadState = derivedStateOf { items.loadState }.value

    @Composable
    fun OnEmpty(body: @Composable () -> Unit) {
        if (handled) return
        if (loadState.refresh !is LoadState.Error && items.itemCount == 0) {
            handled = true
            body()
        }
    }

    @Composable
    fun OnRefresh(body: @Composable () -> Unit) {
        if (handled) return
        if (loadState.refresh is LoadState.Loading) {
            handled = true
            body()
        }
    }

    @Composable
    fun OnSuccess(body: @Composable (LazyPagingItems<T>) -> Unit) {
        if (!handled) {
            handled = true
            body(items)
        }
    }

    @Composable
    fun OnError(body: @Composable (SearchError) -> Unit) {
        if (handled) return
        if (loadState.refresh is LoadState.Error) {
            val result = when (val error = (loadState.refresh as LoadState.Error).error) {
                is NoTransformationFoundException -> SearchError.InvalidResponse
                is DoubleReceiveException -> SearchError.InvalidResponse
                is SocketTimeoutException -> SearchError.NetworkError
                is UnresolvedAddressException -> SearchError.NetworkError
                is ResponseException -> SearchError.ServerError(error.response.status.value)
                else -> SearchError.NetworkError
            }
            handled = true
            body(result)
        }
    }

    @LazyScopeMarker
    fun LazyListScope.onAppendItem(body: @Composable LazyItemScope.() -> Unit) {
        if (loadState.append == LoadState.Loading) {
            item { body(this) }
        }
    }

    @LazyScopeMarker
    fun LazyListScope.onLastItem(body: @Composable LazyItemScope.() -> Unit) {
        if (loadState.append.endOfPaginationReached) item { body(this) }
    }

    @LazyScopeMarker
    fun LazyListScope.onPagingItems(key: ((T) -> Any)?, body: @Composable LazyItemScope.(T) -> Unit) {
        items(
            count = items.itemCount,
            key = items.itemKey(key),
        ) { index ->
            val item = items[index]
            item?.let {
                body(it)
            }
        }
    }
}

@Composable
fun <T : Any> HandlePagingItems(
    items: LazyPagingItems<T>,
    content: @Composable PagingHandlerScope<T>.() -> Unit
) {
    PagingHandlerScope(items).content()
}