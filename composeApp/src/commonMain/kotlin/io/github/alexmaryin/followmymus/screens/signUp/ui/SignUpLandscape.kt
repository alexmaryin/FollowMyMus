package io.github.alexmaryin.followmymus.screens.signUp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.value.Value
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.mbrz_logo_support
import followmymus.composeapp.generated.resources.music_brainz
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpAction
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpState
import io.github.alexmaryin.followmymus.screens.signUp.ui.LogoAnimation
import io.github.alexmaryin.followmymus.screens.signUp.ui.parts.LoginLInk
import io.github.alexmaryin.followmymus.screens.signUp.ui.parts.NicknameField
import io.github.alexmaryin.followmymus.screens.signUp.ui.parts.PasswordField
import io.github.alexmaryin.followmymus.screens.signUp.ui.parts.SignUpButton
import io.github.alexmaryin.followmymus.screens.signUp.ui.parts.SignUpTitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignUpLandscape(
    modifier: Modifier = Modifier,
    stateValue: Value<SignUpState>,
    onAction: (SignUpAction) -> Unit
) {
    val state = stateValue.subscribeAsState()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SignUpTitle(Modifier.align(Alignment.CenterHorizontally))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NicknameField(
                    nickname = state.value.nickname,
                    isNicknameValid = state.value.isNicknameValid
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PasswordField(
                    password = state.value.password,
                    isPasswordVisible = state.value.isPasswordVisible,
                    isPasswordValid = state.value.isPasswordValid
                ) { onAction(SignUpAction.TogglePasswordVisibility) }
            }
            SignUpButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                isEnabled = !state.value.isLoading
            ) { onAction(SignUpAction.OnSignUp) }
            LoginLInk { onAction(SignUpAction.OnOpenLogin) }
        }
        LogoAnimation(
            imageRes = Res.drawable.music_brainz,
            text = stringResource(Res.string.mbrz_logo_support)
        )
    }
}