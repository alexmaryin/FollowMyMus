package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.download
import followmymus.composeapp.generated.resources.qr_code_expires_in
import followmymus.composeapp.generated.resources.qr_gen_support_text
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import qrcode.QRCode
import qrcode.color.Colors

@Composable
fun QRGeneratedBlock(
    deepLink: String,
    onDownloadClick: (ImageBitmap) -> Unit,
    onExpired: () -> Unit
) {
    val painter = QRCode.ofCircles()
        .withBackgroundColor(Colors.WHITE_SMOKE)
        .withSize(15)
        .build(deepLink)
        .render()
    val image = remember { painter.getBytes().decodeToImageBitmap() }

    var animationStarted by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 0.1f else 1f,
        animationSpec = tween(durationMillis = 60_000)
    )

    var timeLeft by remember { mutableStateOf(60) }

    LaunchedEffect(Unit) {
        animationStarted = true
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        onExpired()
    }

    Row(
        modifier = Modifier.fillMaxWidth(0.7f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = image,
            contentDescription = "QR",
            modifier = Modifier
                .weight(1f)
                .sizeIn(maxWidth = 150.dp)
                .padding(6.dp)
                .alpha(alpha)
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.qr_gen_support_text),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
            Icon(
                painter = painterResource(Res.drawable.download),
                contentDescription = "download QR image",
                modifier = Modifier.clickable { onDownloadClick(image) }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.qr_code_expires_in, timeLeft),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun QRGeneratedBlockPreview() {
    Surface {
        QRGeneratedBlock(
            "random string or better UUID",
            {}, {}
        )
    }
}
