package io.github.alexmaryin.followmymus.qrScanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WrongQrHint(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible, enter = fadeIn(), exit = fadeOut(),
        modifier = modifier.padding(vertical = 6.dp)
    ) {
        Text(
            text = "Wrong QR code",
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(8.dp)
        )
    }
}