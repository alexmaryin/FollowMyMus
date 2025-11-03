package io.github.alexmaryin.followmymus.screens.login.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAutofillManager
import io.github.alexmaryin.followmymus.core.ui.DeviceConfiguration
import io.github.alexmaryin.followmymus.core.ui.ObserveEvents
import io.github.alexmaryin.followmymus.screens.login.domain.LoginAction
import io.github.alexmaryin.followmymus.screens.login.domain.LoginComponent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginEvent

@Composable
fun LoginScreen(
    component: LoginComponent
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val deviceConfiguration = DeviceConfiguration.fromWindowSize(windowSize)
    val autofillManager = LocalAutofillManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    fun actionHandler(action: LoginAction) {
        when (action) {
            LoginAction.OnLogin -> {
                autofillManager?.commit()
                component(LoginAction.OnLogin)
            }

            is LoginAction.OnQrRecognized -> component(LoginAction.OnQrRecognized(action.qrCode))

            LoginAction.OnOpenSignUp -> component(LoginAction.OnOpenSignUp)
            LoginAction.OnOpenQrScan -> component(LoginAction.OnOpenQrScan)
            LoginAction.OnCloseQrScan -> component(LoginAction.OnCloseQrScan)
        }
    }

    ObserveEvents(component.events) { event ->
        when (event) {
            is LoginEvent.ShowError -> snackbarHostState.showSnackbar(
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
            DeviceConfiguration.MOBILE_PORTRAIT -> LoginPortrait(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues).consumeWindowInsets(WindowInsets.safeContent),
                stateValue = component.state,
                onAction = ::actionHandler
            )

            DeviceConfiguration.MOBILE_LANDSCAPE -> LoginPhoneLandscape(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues).consumeWindowInsets(WindowInsets.safeContent),
                stateValue = component.state,
                onAction = ::actionHandler
            )

            else -> LoginLandscape(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues).consumeWindowInsets(WindowInsets.safeContent),
                stateValue = component.state,
                onAction = ::actionHandler
            )
        }
    }
}