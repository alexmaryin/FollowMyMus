package io.github.alexmaryin.followmymus.core

open class ErrorType
object UndefinedError : ErrorType()

sealed class Result<out T> {
    data class Success<out R>(val value: R) : Result<R>()
    data class Error(val type: ErrorType, val message: String? = null) : Result<Nothing>()
}

fun <T> Result<T>.requireSuccess() = if (this is Result.Success) value
else throw ClassCastException("Result is not success!")

fun <T> Result<T>.requireError() = if (this is Result.Error) type
else throw ClassCastException("Result is not error!")

inline fun <reified T> Result<T>.forSuccess(callback: (value: T) -> Unit) {
    if (this is Result.Success) callback(value)
}

inline fun <reified T> Result<T>.forError(callback: (type: ErrorType, message: String?) -> Unit) {
    if (this is Result.Error) callback(type, message)
}

inline fun <reified T> Result<T>.forError(callback: (Result.Error) -> Unit) {
    if (this is Result.Error) callback(this)
}

inline fun <T> Result<T>.withDefault(value: () -> T): Result.Success<T> {
    return when (this) {
        is Result.Success -> this
        else -> Result.Success(value())
    }
}