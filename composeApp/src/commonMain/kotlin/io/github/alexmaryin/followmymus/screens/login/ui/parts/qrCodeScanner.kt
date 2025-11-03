package io.github.alexmaryin.followmymus.screens.login.ui.parts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun QRCodeScanner(
    modifier: Modifier = Modifier,
    onQrDetected: (String) -> Unit = {},
    onCancel: () -> Unit = {}
)