package io.github.alexmaryin.followmymus.screens.login.ui.parts

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.nickname
import followmymus.composeapp.generated.resources.nickname_login_label
import io.github.alexmaryin.followmymus.screens.commonUi.BaseTextField
import org.jetbrains.compose.resources.stringResource

@Composable
fun NicknameLoginField(
    nickname: TextFieldState,
    isInvalid: Boolean
) = BaseTextField(
    fieldState = nickname,
    modifier = Modifier.semantics { contentType = ContentType.Username },
    overheadText = stringResource(Res.string.nickname),
    labelText = stringResource(Res.string.nickname_login_label),
    isError = isInvalid
)