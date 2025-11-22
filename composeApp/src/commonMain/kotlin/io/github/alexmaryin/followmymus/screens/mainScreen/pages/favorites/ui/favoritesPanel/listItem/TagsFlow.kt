package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.MusicTag

@Composable
fun TagsFlow(tags: List<MusicTag>) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    maxLines = 4
) {
    tags.forEach { tag ->
        AssistChip({}, label = { Text(text = tag.name) })
    }
}