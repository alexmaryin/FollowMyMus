package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components

import androidx.compose.runtime.Composable
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.models.PreferencesItem

@Composable
fun PreferencesGroup(
    groupCaption: String,
    vararg items: PreferencesItem
) {
    GroupCaption(groupCaption)
    SoftCornerBlock {
        items.dropLast(1).forEach { preferencesItem ->
            PreferenceListItem(preferencesItem.copy(withDivider = true))
        }
        PreferenceListItem(items.last().copy(withDivider = false))
    }
}

