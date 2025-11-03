package io.github.alexmaryin.followmymus.screens.login.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.value.Value
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.mbrz_logo_support
import followmymus.composeapp.generated.resources.music_brainz
import io.github.alexmaryin.followmymus.screens.commonUi.LogoAnimation
import io.github.alexmaryin.followmymus.screens.login.domain.LoginAction
import io.github.alexmaryin.followmymus.screens.login.domain.LoginState
import io.github.alexmaryin.followmymus.screens.login.ui.parts.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginPhoneLandscape(
    modifier: Modifier = Modifier,
    stateValue: Value<LoginState>,
    onAction: (LoginAction) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    LoginTitle(Modifier.align(Alignment.CenterHorizontally), wideSize = false)
                    if (!state.value.isQrScanOpen)
                        QrScanButton(Modifier.align(Alignment.CenterHorizontally)) {
                            onAction(LoginAction.OnOpenQrScan)
                        }
                    SignUpLink(Modifier.align(Alignment.CenterHorizontally)) {
                        onAction(LoginAction.OnOpenSignUp)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    if (state.value.isQrScanOpen) {
                        QRCodeScanner(
                            modifier = Modifier.fillMaxSize()
                                .wrapContentSize(Alignment.Center)
                                .size(250.dp),
                            onQrDetected = { qrCode -> onAction(LoginAction.OnQrRecognized(qrCode)) },
                            onCancel = { onAction(LoginAction.OnCloseQrScan) }
                        )
                    } else {
                        NicknameLoginField(
                            nickname = state.value.nickname,
                            isInvalid = !state.value.isCredentialsValid,
                            onLoginChange = { new -> onAction(LoginAction.OnNickNameSet(new)) }
                        )
                        PasswordLoginField(
                            password = state.value.password,
                            isInvalid = !state.value.isCredentialsValid,
                            onPasswordChange = { new -> onAction(LoginAction.OnPasswordSet(new)) }
                        )
                        LoginButton(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            isEnabled = !state.value.isLoading
                        ) { onAction(LoginAction.OnLogin) }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomStart).fillMaxSize(0.5f)) {
            LogoAnimation(
                imageRes = Res.drawable.music_brainz,
                text = stringResource(Res.string.mbrz_logo_support)
            )
        }
    }
}