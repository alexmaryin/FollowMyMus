package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.more_vert
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoriteListAction
import org.jetbrains.compose.resources.painterResource

@Composable
fun ActionsIcon(onAction: (FavoriteListAction) -> Unit) {
    var actionsMenuVisible by remember { mutableStateOf(false) }

    Icon(
        painter = painterResource(Res.drawable.more_vert),
        contentDescription = "favorite artist actions",
        modifier = Modifier
            .clip(CircleShape)
            .clickable { actionsMenuVisible = !actionsMenuVisible }
    )
}