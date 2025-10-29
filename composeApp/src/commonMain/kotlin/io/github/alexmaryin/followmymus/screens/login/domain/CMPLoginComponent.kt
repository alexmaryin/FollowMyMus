package io.github.alexmaryin.followmymus.screens.login.domain

import com.arkivanov.decompose.ComponentContext
import org.koin.core.annotation.Factory

@Factory
class CMPLoginComponent(
    private val qrCode: String?,
    private val componentContext: ComponentContext,
    private val onSighUpClick: () -> Unit
) : LoginComponent, ComponentContext by componentContext {

}