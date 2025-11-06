package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun GroupCaption(caption: String) {
    Text(
        text = caption,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primaryFixedDim,
        modifier = Modifier.fillMaxWidth()
            .padding(6.dp),
        textAlign = TextAlign.Start
    )
}