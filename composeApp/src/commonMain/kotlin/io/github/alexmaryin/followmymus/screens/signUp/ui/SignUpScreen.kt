package io.github.alexmaryin.followmymus.screens.signUp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAutofillManager
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.mbrz_logo_support
import followmymus.composeapp.generated.resources.music_brainz
import io.github.alexmaryin.followmymus.core.ui.DeviceConfiguration
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpAction
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpComponent
import io.github.alexmaryin.followmymus.screens.signUp.ui.parts.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignUpScreen(
    component: SignUpComponent
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val deviceConfiguration = DeviceConfiguration.fromWindowSize(windowSize)
    val autofillManager = LocalAutofillManager.current

    fun actionHandler(action: SignUpAction) {
        when (action) {
            SignUpAction.OnOpenLogin -> component(SignUpAction.OnOpenLogin)
            SignUpAction.OnSignUp -> {
                autofillManager?.commit()
                component(SignUpAction.OnSignUp)
            }

            SignUpAction.TogglePasswordVisibility -> component(SignUpAction.TogglePasswordVisibility)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) { paddingValues ->

        when (deviceConfiguration) {
            DeviceConfiguration.MOBILE_PORTRAIT, DeviceConfiguration.TABLET_PORTRAIT -> {
                SignUpPortrait(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues).consumeWindowInsets(WindowInsets.safeContent),
                    stateValue = component.state,
                    onAction = ::actionHandler
                )
            }

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