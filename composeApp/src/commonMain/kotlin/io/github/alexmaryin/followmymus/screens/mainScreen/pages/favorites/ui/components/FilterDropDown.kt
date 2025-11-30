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
import followmymus.composeapp.generated.resources.sort_artists_caption
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import org.jetbrains.compose.resources.stringResource

@Composable
fun FilterDropDown(
    isVisible: Boolean,
    selected: SortArtists,
    onDismiss: () -> Unit,
    onChange: (SortArtists) -> Unit
) {
    DropdownMenu(
        expanded = isVisible,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = stringResource(Res.string.sort_artists_caption),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
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
                            onChange(new)
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
        }
    }
}

@Preview
@Composable
fun FilterDropDownPreview() {
    Surface(
        Modifier.fillMaxSize()
    ) {
        FilterDropDown(
            isVisible = true,
            selected = SortArtists.NONE,
            onDismiss = {},
            onChange = {}
        )
    }
}
