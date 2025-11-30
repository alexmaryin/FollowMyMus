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
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.FilterDropDown
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FavoritesListTitle(
    modifier: Modifier = Modifier,
    selectedSorting: SortArtists,
    onFilterChange: (SortArtists) -> Unit
) {
    var isFilterVisible by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(selectedSorting) }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { isFilterVisible = !isFilterVisible },
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.tune),
                contentDescription = "filter artists"
            )
            FilterDropDown(
                isVisible = isFilterVisible,
                selected = selectedFilter,
                onDismiss = { isFilterVisible = false },
                onChange = { new ->
                    selectedFilter = new
                    isFilterVisible = false
                    onFilterChange(new)
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