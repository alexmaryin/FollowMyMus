package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.refresh_icon
import followmymus.composeapp.generated.resources.sort_artists_caption
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FavoritesActions(
    isVisible: Boolean,
    selected: SortArtists,
    isRefreshEnabled: Boolean,
    onDismiss: () -> Unit,
    onSortChange: (SortArtists) -> Unit,
    onRefresh: () -> Unit
) {
    DropdownMenu(
        expanded = isVisible,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(Res.string.sort_artists_caption),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SortArtists.entries.drop(1).forEachIndexed { index, filter ->
                    SegmentedButton(
                        modifier = Modifier.defaultMinSize(minWidth = 100.dp),
                        selected = filter == selected,
                        onClick = {
                            val new = if (filter == selected) SortArtists.NONE else filter
                            onSortChange(new)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, SortArtists.entries.size - 1),
                        label = {
                            Text(
                                text = stringResource(filter.title),
                                maxLines = 1
                            )
                        },
                    )
                }
            }
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                enabled = isRefreshEnabled,
                border = SegmentedButtonDefaults.borderStroke( MaterialTheme.colorScheme.outline)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.refresh_icon),
                    contentDescription = null
                )
                Text(text = "Refresh releases list")
            }
        }
    }
}

@Preview
@Composable
fun FilterDropDownPreview() {
    Surface(
        Modifier.fillMaxSize()
    ) {
        FavoritesActions(
            isVisible = true,
            selected = SortArtists.NONE,
            isRefreshEnabled = false,
            onDismiss = {},
            onSortChange = {},
            onRefresh = {},
        )
    }
}
