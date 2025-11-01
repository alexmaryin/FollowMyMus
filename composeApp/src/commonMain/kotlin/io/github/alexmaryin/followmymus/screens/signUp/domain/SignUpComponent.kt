package io.github.alexmaryin.followmymus.screens.signUp.domain

import com.arkivanov.decompose.value.Value

interface SignUpComponent {
    val state: Value<SignUpState>

    operator fun invoke(action: SignUpAction)
}