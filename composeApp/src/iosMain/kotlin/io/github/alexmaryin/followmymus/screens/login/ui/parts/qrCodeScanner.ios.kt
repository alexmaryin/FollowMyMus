package io.github.alexmaryin.followmymus.screens.login.ui.parts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun QRCodeScanner(
    modifier: Modifier,
    onQrDetected: (String) -> Unit,
    onCancel: () -> Unit
) {
}