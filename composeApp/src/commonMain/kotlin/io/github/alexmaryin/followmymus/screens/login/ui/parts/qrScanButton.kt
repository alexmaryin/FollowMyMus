package io.github.alexmaryin.followmymus.screens.login.ui.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.login_qr_text
import followmymus.composeapp.generated.resources.qr_code_scanner
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun QrScanButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = Button(
    onClick = onClick,
    modifier = modifier
        .sizeIn(maxWidth = 300.dp)
        .fillMaxWidth()
        .padding(16.dp),
    colors = ButtonDefaults.buttonColors().copy(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
) {
    Icon(
        painter = painterResource(Res.drawable.qr_code_scanner),
        contentDescription = "scan QR to login"
    )
    Text(
        text = stringResource(Res.string.login_qr_text),
        modifier = Modifier.padding(6.dp)
    )
}