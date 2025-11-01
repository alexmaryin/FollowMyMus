package io.github.alexmaryin.followmymus.screens.signUp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpAction
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpComponent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignUpScreen(
    component: SignUpComponent
) {
    val state = component.state.subscribeAsState()
    val autofillManager = LocalAutofillManager.current

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeContent)
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues).padding(bottom = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(Res.string.sign_up_title),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    state = state.value.nickname,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                        .semantics { ContentType.NewUsername },
                    label = { Text(text = stringResource(Res.string.nickname_signup_label)) },
                    trailingIcon = {
                        if (state.value.nickname.text.isNotEmpty())
                            Icon(
                                painter = painterResource(Res.drawable.cancel),
                                contentDescription = "clear nickname",
                                modifier = Modifier.clickable {
                                    state.value.nickname.clearText()
                                }
                            )
                    },
                    supportingText = { Text(text = stringResource(Res.string.nickname_signup_support)) },
                    isError = !state.value.isNicknameValid,
                    lineLimits = TextFieldLineLimits.SingleLine
                )
                OutlinedSecureTextField(
                    state = state.value.password,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                        .semantics { ContentType.NewPassword },
                    label = { Text(text = stringResource(Res.string.password_signup_label)) },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(
                                if (state.value.isPasswordVisible)
                                    Res.drawable.visibility_off else Res.drawable.visibility
                            ),
                            contentDescription = "toggle password visibility",
                            modifier = Modifier.clickable {
                                component(SignUpAction.TogglePasswordVisibility)
                            }
                        )
                    },
                    textObfuscationMode = if (state.value.isPasswordVisible) TextObfuscationMode.Visible
                    else TextObfuscationMode.RevealLastTyped,
                    supportingText = { Text(text = stringResource(Res.string.password_signup_support)) },
                    isError = !state.value.isPasswordValid,
                )
                Button(
                    enabled = !state.value.isLoading,
                    onClick = {
                        autofillManager?.commit()
                        component(SignUpAction.OnSignUp)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.6f).padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.sign_up),
                        contentDescription = "sign up click"
                    )
                    Text(
                        text = stringResource(Res.string.sign_up_button),
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(Res.string.label_to_login),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.link_to_login),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            component(SignUpAction.OnOpenLogin)
                        }
                    )
                }
            }

            LogoAnimation(
                imageRes = Res.drawable.music_brainz,
                text = stringResource(Res.string.mbrz_logo_support)
            )
        }
    }
}