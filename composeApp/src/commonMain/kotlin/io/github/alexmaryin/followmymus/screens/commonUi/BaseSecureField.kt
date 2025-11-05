package io.github.alexmaryin.followmymus.screens.commonUi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.visibility
import followmymus.composeapp.generated.resources.visibility_off
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.painterResource

val isPasswordVisibleKey = SemanticsPropertyKey<Boolean>("isPasswordVisible")
var SemanticsPropertyReceiver.isPasswordVisible by isPasswordVisibleKey

@Composable
fun BaseSecureField(
    fieldState: TextFieldState,
    modifier: Modifier = Modifier,
    overheadText: String? = null,
    labelText: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardController: SoftwareKeyboardController? = null,
    onTextChange: (String) -> Unit = {}
) {
    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }.collectLatest {
            onTextChange(it)
        }
    }
    var isVisible by remember { mutableStateOf(false) }
    val keyboardController = keyboardController ?: LocalSoftwareKeyboardController.current

    overheadText?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
    }
    OutlinedSecureTextField(
        state = fieldState,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            .sizeIn(maxWidth = 600.dp)
            .fillMaxWidth()
            .semantics { isPasswordVisible = isVisible },
        label = {
            labelText?.let { Text(text = it) }
        },
        trailingIcon = {
            Icon(
                painter = painterResource(
                    if (isVisible) Res.drawable.visibility_off else Res.drawable.visibility
                ),
                contentDescription = "toggle password visibility",
                modifier = Modifier.clickable { isVisible = !isVisible }
            )
        },
        textObfuscationMode = if (isVisible) TextObfuscationMode.Visible
        else TextObfuscationMode.RevealLastTyped,
        supportingText = {
            supportingText?.let { Text(it) }
        },
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        onKeyboardAction = { keyboardController?.hide() }
    )
}