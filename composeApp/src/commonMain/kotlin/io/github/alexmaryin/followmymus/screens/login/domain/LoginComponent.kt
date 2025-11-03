package io.github.alexmaryin.followmymus.screens.login.domain

import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow

interface LoginComponent {
    val state: Value<LoginState>
    val events: Flow<LoginEvent>

    operator fun invoke(action: LoginAction)
}