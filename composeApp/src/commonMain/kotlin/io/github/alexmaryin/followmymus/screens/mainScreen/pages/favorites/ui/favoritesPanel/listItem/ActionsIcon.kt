package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.more_vert
import followmymus.composeapp.generated.resources.sync_pend_push
import followmymus.composeapp.generated.resources.sync_pend_remove
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.FavoritesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.setToRemove
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import org.jetbrains.compose.resources.painterResource

@Composable
fun ActionsIcon(
    artist: FavoriteArtist,
    onAction: (FavoritesListAction) -> Unit
) {
    var actionsMenuVisible by remember { mutableStateOf(false) }

    Icon(
        painter = painterResource(Res.drawable.more_vert),
        contentDescription = "favorite artist actions",
        modifier = Modifier
            .clip(CircleShape)
            .clickable { actionsMenuVisible = !actionsMenuVisible }
    )

    DropdownMenu(
        expanded = actionsMenuVisible,
        onDismissRequest = { actionsMenuVisible = false },
        shape = RoundedCornerShape(24.dp)
    ) {
        DropdownMenuItem(
            text = { Text("Remove from favorites") },
            onClick = {
                actionsMenuVisible = false
                onAction(FavoritesListAction.OpenConfirmToRemove(artist.setToRemove()))
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.sync_pend_remove),
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Update details") },
            onClick = {
                actionsMenuVisible = false
                onAction(FavoritesListAction.UpdateDetails(artist.id))
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.sync_pend_push),
                    contentDescription = null
                )
            }
        )
    }
}