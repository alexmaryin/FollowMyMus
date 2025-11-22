package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

fun artistDescription(description: String?) = @Composable {
    description?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}