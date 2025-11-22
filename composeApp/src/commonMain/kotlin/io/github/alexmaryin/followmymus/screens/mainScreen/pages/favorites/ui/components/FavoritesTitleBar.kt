package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.app_name
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.nicknameAvatar.AvatarState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.nicknameAvatar.Avatar
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FavoritesTitleBar(
    modifier: Modifier = Modifier,
    avatarState: AvatarState,
    onSyncRequest: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(4.dp)
        )
        Avatar(
            state = avatarState,
            modifier = Modifier.padding(4.dp),
            onSyncRequest = onSyncRequest
        )
    }
}

@Preview
@Composable
fun FavoritesTitlePreview() {
    Surface {
        FavoritesTitleBar(
            avatarState = AvatarState("Alex"),
            onSyncRequest = {}
        )
    }
}