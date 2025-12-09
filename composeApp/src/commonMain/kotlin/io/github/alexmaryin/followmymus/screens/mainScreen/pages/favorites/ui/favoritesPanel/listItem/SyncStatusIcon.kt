package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.sync_ok
import followmymus.composeapp.generated.resources.sync_pend_push
import followmymus.composeapp.generated.resources.sync_pend_remove
import io.github.alexmaryin.followmymus.core.ui.theme.FollowMyMusTheme
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SyncStatus
import org.jetbrains.compose.resources.painterResource

@Composable
fun SyncStatusIcon(syncStatus: SyncStatus, modifier: Modifier = Modifier) {
    val (iconRes, iconColor) = when (syncStatus) {
        SyncStatus.PendingRemoteAdd -> Res.drawable.sync_pend_push to MaterialTheme.colorScheme.primary
        SyncStatus.PendingRemoteRemove -> Res.drawable.sync_pend_remove to MaterialTheme.colorScheme.primary
        SyncStatus.OK -> Res.drawable.sync_ok to MaterialTheme.colorScheme.outlineVariant
    }
    val badged = syncStatus != SyncStatus.OK
    BadgedBox(
        badge = { if (badged) Badge() }
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconColor.copy(alpha = 0.6f),
            modifier = modifier
        )
    }
}

@Preview
@Composable
fun SyncStatusIconPreview() {
    FollowMyMusTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SyncStatusIcon(SyncStatus.OK)
                SyncStatusIcon(SyncStatus.PendingRemoteAdd)
                SyncStatusIcon(SyncStatus.PendingRemoteRemove)
            }
        }
    }

}