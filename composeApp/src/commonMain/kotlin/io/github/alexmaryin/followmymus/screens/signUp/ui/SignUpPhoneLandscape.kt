package io.github.alexmaryin.followmymus.screens.signUp.ui

import androidx.compose.foundation.layout.*
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
import io.github.alexmaryin.followmymus.screens.commonUi.LogoAnimation
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpAction
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpState
import io.github.alexmaryin.followmymus.screens.signUp.ui.parts.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignUpPhoneLandscape(
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
            SignUpTitle(Modifier.align(Alignment.Start), wideSize = false)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    NicknameField(
                        nickname = state.value.nickname,
                        isNicknameValid = state.value.isNicknameValid
                    ) { onAction(SignUpAction.NicknameChange(it)) }
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    PasswordField(
                        password = state.value.password,
                        isPasswordValid = state.value.isPasswordValid,
                    ) { onAction(SignUpAction.PasswordChange(it)) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                SignUpButton(
                    modifier = Modifier.weight(1f),
                    isEnabled = !state.value.isLoading
                ) { onAction(SignUpAction.OnSignUp) }
                LoginLink(Modifier.weight(1f)) { onAction(SignUpAction.OnOpenLogin) }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).fillMaxSize(0.5f)) {
            LogoAnimation(
                imageRes = Res.drawable.music_brainz,
                text = stringResource(Res.string.mbrz_logo_support)
            )
        }
    }
}