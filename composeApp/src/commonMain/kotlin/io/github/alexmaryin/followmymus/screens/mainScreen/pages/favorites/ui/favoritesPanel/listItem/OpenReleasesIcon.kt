package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.forward
import org.jetbrains.compose.resources.painterResource

@Composable
fun OpenReleasesIcon(isSelected: Boolean, onClick: () -> Unit) {
    val rotation = animateFloatAsState(
        targetValue = if (isSelected) 180f else 0f, label = "rotation180"
    )
    
    Icon(
        painter = painterResource(Res.drawable.forward),
        contentDescription = "open releases",
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .rotate(rotation.value)
    )
}