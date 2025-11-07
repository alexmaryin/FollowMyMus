package io.github.alexmaryin.followmymus.screens.login.ui.parts.qrScanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.sessionManager.data.qrcode.DEEP_LINK_URL_PREFIX
import kotlinx.coroutines.delay

@Composable
fun CameraWithScanner(
    modifier: Modifier = Modifier,
    onQrDetected: (String) -> Unit,
    onCancel: () -> Unit
) {
    var hintWrongQR by remember { mutableStateOf(false) }
    var hasScanned by remember { mutableStateOf(false) }

    // If scanned QR not related to this app, it will show hint for 2 seconds
    LaunchedEffect(hintWrongQR) {
        if (hintWrongQR) {
            delay(2000L)
            hintWrongQR = false
        }
    }
    Box(modifier = modifier) {
        CameraView(
            modifier = Modifier.align(Alignment.Center),
            onQrScanned = { value ->
                if (value.startsWith(DEEP_LINK_URL_PREFIX)) {
                    if (!hasScanned) {
                        hasScanned = true
                        onQrDetected(value)
                    }
                } else if (!hintWrongQR) hintWrongQR = true
            }
        )

        WrongQrHint(visible = hintWrongQR, modifier = Modifier.align(Alignment.TopCenter))

        Button(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(0.5f)
                .padding(6.dp)
        ) {
            Text("Cancel")
        }
    }
}