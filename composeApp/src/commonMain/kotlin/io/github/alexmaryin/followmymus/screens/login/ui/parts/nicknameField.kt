package io.github.alexmaryin.followmymus.screens.login.ui.parts

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.nickname
import followmymus.composeapp.generated.resources.nickname_login_label
import io.github.alexmaryin.followmymus.screens.commonUi.BaseTextField
import org.jetbrains.compose.resources.stringResource

@Composable
fun NicknameLoginField(
    nickname: String,
    isInvalid: Boolean,
    onLoginChange: (String) -> Unit
) = BaseTextField(
    fieldState = rememberTextFieldState(nickname),
    modifier = Modifier.semantics { contentType = ContentType.Username },
    overheadText = stringResource(Res.string.nickname),
    labelText = stringResource(Res.string.nickname_login_label),
    isError = isInvalid,
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next
    ),
    onTextChange = onLoginChange
)