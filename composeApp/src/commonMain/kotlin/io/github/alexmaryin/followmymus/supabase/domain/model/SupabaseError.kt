package io.github.alexmaryin.followmymus.supabase.domain.model

import io.github.alexmaryin.followmymus.core.ErrorType

sealed class SupabaseError : ErrorType() {
    data class SupabaseResponseError(val message: String?, val hint: String?) : SupabaseError()
    data class SupabasNetworkError(val message: String?) : SupabaseError()
    data object SupabaseTimeout : SupabaseError()
    data object InvalidJsonResponse : SupabaseError()
    data object MappingError : SupabaseError()
}
