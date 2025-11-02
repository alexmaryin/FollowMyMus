package io.github.alexmaryin.followmymus.screens.signUp.ui.parts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordField(
    password: TextFieldState,
    isPasswordVisible: Boolean,
    isPasswordValid: Boolean,
    onToggleVisibility: () -> Unit
) {
    Text(
        text = stringResource(Res.string.password),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
    OutlinedSecureTextField(
        state = password,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            .sizeIn(maxWidth = 600.dp)
            .fillMaxWidth()
            .semantics { ContentType.NewPassword },
        label = { Text(text = stringResource(Res.string.password_signup_label)) },
        trailingIcon = {
            Icon(
                painter = painterResource(
                    if (isPasswordVisible) Res.drawable.visibility_off else Res.drawable.visibility
                ),
                contentDescription = "toggle password visibility",
                modifier = Modifier.clickable { onToggleVisibility() }
            )
        },
        textObfuscationMode = if (isPasswordVisible) TextObfuscationMode.Visible
        else TextObfuscationMode.RevealLastTyped,
        supportingText = { Text(text = stringResource(Res.string.password_signup_support)) },
        isError = !isPasswordValid,
    )
}