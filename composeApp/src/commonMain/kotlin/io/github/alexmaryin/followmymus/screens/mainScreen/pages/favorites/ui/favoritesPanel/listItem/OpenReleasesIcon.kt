package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.forward
import org.jetbrains.compose.resources.painterResource

@Composable
fun OpenReleasesIcon(onClick: () -> Unit) {
    Icon(
        painter = painterResource(Res.drawable.forward),
        contentDescription = "open releases",
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    )
}