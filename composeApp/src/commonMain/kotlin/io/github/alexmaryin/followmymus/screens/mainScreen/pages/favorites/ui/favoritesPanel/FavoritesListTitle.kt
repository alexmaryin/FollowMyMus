package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.app_name
import followmymus.composeapp.generated.resources.tune
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.FavoritesActions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FavoritesListTitle(
    modifier: Modifier = Modifier,
    selectedSorting: SortArtists,
    isRefreshEnabled: Boolean,
    onFilterChange: (SortArtists) -> Unit,
    onRefresh: () -> Unit
) {
    var isActionsVisible by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { isActionsVisible = !isActionsVisible },
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.tune),
                contentDescription = "filter artists"
            )
            FavoritesActions(
                isVisible = isActionsVisible,
                selected = selectedSorting,
                isRefreshEnabled = isRefreshEnabled,
                onDismiss = { isActionsVisible = false },
                onSortChange = { new ->
                    isActionsVisible = false
                    onFilterChange(new)
                },
                onRefresh = {
                    isActionsVisible = false
                    onRefresh()
                }
            )
        }
        Text(
            stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = modifier.padding(4.dp)
        )
    }
}