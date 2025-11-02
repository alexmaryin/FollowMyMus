package io.github.alexmaryin.followmymus.screens.signUp.domain

import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow

interface SignUpComponent {
    val state: Value<SignUpState>
    val events: Flow<SignUpEvent>

    operator fun invoke(action: SignUpAction)
}