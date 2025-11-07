package io.github.alexmaryin.followmymus.screens.commonUi

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.cancel_dialog_button
import followmymus.composeapp.generated.resources.ok_dialog_button
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    confirmButtonText: String = stringResource(Res.string.ok_dialog_button),
    dismissButtonText: String = stringResource(Res.string.cancel_dialog_button),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText)
            }
        }
    )
}
