package io.github.alexmaryin.followmymus.screens.signUp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import io.github.alexmaryin.followmymus.core.ui.DeviceConfiguration
import io.github.alexmaryin.followmymus.core.ui.ObserveEvents
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpAction
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpComponent
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpEvent

@Composable
fun SignUpScreen(
    component: SignUpComponent
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val deviceConfiguration = DeviceConfiguration.fromWindowSize(windowSize)
    val autofillManager = LocalAutofillManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun actionHandler(action: SignUpAction) {
        when (action) {
            SignUpAction.OnSignUp -> {
                keyboardController?.hide()
                autofillManager?.commit()
                component(SignUpAction.OnSignUp)
            }
            else -> component(action)
        }
    }

    ObserveEvents(component.events) { event ->
        when (event) {
            is SignUpEvent.ShowError -> snackbarHostState.showSnackbar(
                message = event.message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        when (deviceConfiguration) {
            DeviceConfiguration.MOBILE_PORTRAIT -> {
                SignUpPortrait(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues).consumeWindowInsets(WindowInsets.safeContent),
                    stateValue = component.state,
                    onAction = ::actionHandler
                )
            }

            DeviceConfiguration.MOBILE_LANDSCAPE -> {
                SignUpPhoneLandscape(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues).consumeWindowInsets(WindowInsets.safeContent),
                    stateValue = component.state,
                    onAction = ::actionHandler
                )
            }
            // Desktop and any other wide screens
            else -> {
                SignUpLandscape(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues).consumeWindowInsets(WindowInsets.safeContent),
                    stateValue = component.state,
                    onAction = ::actionHandler
                )
            }
        }
    }
}