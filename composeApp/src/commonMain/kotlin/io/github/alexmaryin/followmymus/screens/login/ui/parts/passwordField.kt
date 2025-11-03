package io.github.alexmaryin.followmymus.screens.login.ui.parts

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.password
import followmymus.composeapp.generated.resources.password_login_label
import io.github.alexmaryin.followmymus.screens.commonUi.BaseSecureField
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordLoginField(
    password: String,
    isInvalid: Boolean,
    onPasswordChange: (String) -> Unit
) = BaseSecureField(
    fieldState = rememberTextFieldState(password),
    modifier = Modifier.semantics { contentType = ContentType.Password },
    overheadText = stringResource(Res.string.password),
    labelText = stringResource(Res.string.password_login_label),
    isError = isInvalid,
    onTextChange = onPasswordChange
)