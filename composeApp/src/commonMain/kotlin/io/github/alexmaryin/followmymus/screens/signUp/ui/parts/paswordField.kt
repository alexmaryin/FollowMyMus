package io.github.alexmaryin.followmymus.screens.signUp.ui.parts

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.screens.commonUi.BaseSecureField
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordField(
    password: String,
    isPasswordValid: Boolean,
    onTextChange: (String) -> Unit = {}
) = BaseSecureField(
    fieldState = rememberTextFieldState(password),
    modifier = Modifier.semantics { contentType = ContentType.NewPassword },
    overheadText = stringResource(Res.string.password),
    labelText = stringResource(Res.string.password_signup_label),
    supportingText = stringResource(
        if (!isPasswordValid) Res.string.password_signup_error else
            Res.string.password_signup_support
    ),
    isError = !isPasswordValid,
    onTextChange = onTextChange
)