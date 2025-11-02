package io.github.alexmaryin.followmymus.screens.signUp.ui.parts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.cancel
import followmymus.composeapp.generated.resources.nickname
import followmymus.composeapp.generated.resources.nickname_signup_label
import followmymus.composeapp.generated.resources.nickname_signup_support
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NicknameField(
    nickname: TextFieldState,
    isNicknameValid: Boolean
) {
    Text(
        text = stringResource(Res.string.nickname),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
    OutlinedTextField(
        state = nickname,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            .sizeIn(maxWidth = 600.dp)
            .fillMaxWidth()
            .semantics { ContentType.NewUsername },
        label = { Text(text = stringResource(Res.string.nickname_signup_label)) },
        trailingIcon = {
            if (nickname.text.isNotEmpty())
                Icon(
                    painter = painterResource(Res.drawable.cancel),
                    contentDescription = "clear nickname",
                    modifier = Modifier.clickable {
                        nickname.clearText()
                    }
                )
        },
        supportingText = { Text(text = stringResource(Res.string.nickname_signup_support)) },
        isError = !isNicknameValid,
        lineLimits = TextFieldLineLimits.SingleLine
    )
}