package io.github.alexmaryin.followmymus.screens.signUp.domain

import com.arkivanov.decompose.ComponentContext
import org.koin.core.annotation.Factory

@Factory
class CMPSignUpComponent(
    private val componentContext: ComponentContext,
    private val onLoginClick: () -> Unit
) : SignUpComponent, ComponentContext by componentContext {
}