package io.github.alexmaryin.followmymus.screens.commonUi

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.cancel
import org.jetbrains.compose.resources.painterResource

@Composable
fun BaseTextField(
    fieldState: TextFieldState,
    modifier: Modifier = Modifier,
    overheadText: String? = null,
    labelText: String? = null,
    supportingText: String? = null,
    isError: Boolean,
    onTextChange: (String) -> Unit = {}
) {
    LaunchedEffect(fieldState.text) {
        onTextChange(fieldState.text.toString())
    }
    overheadText?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
    }
    OutlinedTextField(
        state = fieldState,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            .sizeIn(maxWidth = 600.dp)
            .fillMaxWidth(),
        label = {
            labelText?.let { Text(text = it) }
        },
        trailingIcon = {
            if (fieldState.text.isNotEmpty())
                Icon(
                    painter = painterResource(Res.drawable.cancel),
                    contentDescription = "clear text field",
                    modifier = Modifier.clickable {
                        fieldState.clearText()
                    }
                )
        },
        supportingText = {
            supportingText?.let { Text(it) }
        },
        isError = isError,
        lineLimits = TextFieldLineLimits.SingleLine
    )
}