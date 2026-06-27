package io.github.alexmaryin.followmymus.core.paging

/**
 * Domain-agnostic error wrapper used by the paging library.
 *
 * `core` owns this type so the paging wrapper does not depend on
 * any feature module. Each screen maps its own exception types
 * into [PagingError] via a caller-provided `errorMapper`.
 */
sealed interface PagingError {
    data class Network(val message: String?) : PagingError
    data class Server(val code: Int, val message: String) : PagingError
    data object InvalidResponse : PagingError
    data class Unknown(val message: String?) : PagingError
}

/**
 * Default mapper: callers that don't supply their own `errorMapper`
 * get every exception wrapped as [PagingError.Unknown].
 */
fun defaultPagingError(throwable: Throwable): PagingError =
    PagingError.Unknown(throwable.message)
