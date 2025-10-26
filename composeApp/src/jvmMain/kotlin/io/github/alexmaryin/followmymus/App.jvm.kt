package io.github.alexmaryin.followmymus

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import qrcode.QRCode
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
actual fun QRCodeBlock(modifier: Modifier) {
    val transferId = Uuid.random().toString()
    println("QR CODE: $transferId")
    val painter = QRCode.ofCircles()
        .withSize(10)
        .build(transferId)
        .render()
    val image = Image.makeFromEncoded(painter.getBytes()).toComposeImageBitmap()

    Image(
        bitmap = image,
        contentDescription = "QR code for login in mobile app",
        modifier = modifier
    )
}