package io.github.alexmaryin.followmymus.screens.signUp.ui.parts

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.nickname
import followmymus.composeapp.generated.resources.nickname_signup_label
import followmymus.composeapp.generated.resources.nickname_signup_support
import io.github.alexmaryin.followmymus.screens.commonUi.BaseTextField
import org.jetbrains.compose.resources.stringResource

@Composable
fun NicknameField(
    nickname: TextFieldState,
    isNicknameValid: Boolean,
    onTextChange: (String) -> Unit = {}
)  = BaseTextField(
    fieldState = nickname,
    modifier = Modifier.semantics { contentType = ContentType.NewUsername },
    overheadText = stringResource(Res.string.nickname),
    labelText = stringResource(Res.string.nickname_signup_label),
    supportingText = stringResource(Res.string.nickname_signup_support),
    isError = !isNicknameValid,
    onTextChange = onTextChange
)