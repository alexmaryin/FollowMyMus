package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.forward
import org.jetbrains.compose.resources.painterResource

@Composable
fun ArtistTrailingIcons(
    score: Int?,
    name: String,
    isOpened: Boolean,
    onClick: () -> Unit
) {
    val rotation = animateFloatAsState(
        targetValue = if (isOpened) 180f else 0f, label = "rotation180"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        score?.let {
            Text(
                text = it.toString(),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Icon(
            painter = painterResource(Res.drawable.forward),
            contentDescription = "Open releases for $name",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .rotate(rotation.value),
        )
    }
}