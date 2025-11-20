package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.nicknameAvatar.AvatarState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.nicknameAvatar.Avatar

@Composable
fun FavoritesTitleBar(
    modifier: Modifier = Modifier,
    avatarState: AvatarState,
    onSyncRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
    ) {
        Avatar(
            state = avatarState,
            modifier = modifier.padding(4.dp),
            onSyncRequest = onSyncRequest
        )
    }
}