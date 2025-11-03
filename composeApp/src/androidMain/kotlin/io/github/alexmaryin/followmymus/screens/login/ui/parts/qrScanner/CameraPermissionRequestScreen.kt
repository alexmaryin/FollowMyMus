package io.github.alexmaryin.followmymus.screens.login.ui.parts.qrScanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.camera_permission_cancel
import followmymus.composeapp.generated.resources.camera_permission_grant
import followmymus.composeapp.generated.resources.camera_permission_request
import org.jetbrains.compose.resources.stringResource

@Composable
fun CameraPermissionRequestScreen(
    onGrantPermission: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(Res.string.camera_permission_request),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onGrantPermission) {
            Text(stringResource(Res.string.camera_permission_grant))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel) {
            Text(stringResource(Res.string.camera_permission_cancel))
        }
    }
}