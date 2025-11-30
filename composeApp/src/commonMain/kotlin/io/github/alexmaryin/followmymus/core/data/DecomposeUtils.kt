package io.github.alexmaryin.followmymus.core.data

import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Converts a Decompose [Value] to a hot [Flow].
 */
fun <T : Any> Value<T>.asFlow(): Flow<T> {
    val flow = MutableStateFlow(value)
    subscribe { flow.value = it }
    return flow
}
