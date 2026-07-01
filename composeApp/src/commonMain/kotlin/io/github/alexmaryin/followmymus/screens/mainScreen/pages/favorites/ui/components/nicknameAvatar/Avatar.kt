package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.nicknameAvatar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.download
import followmymus.composeapp.generated.resources.favorites_export_menu_item
import followmymus.composeapp.generated.resources.favorites_import_menu_item
import followmymus.composeapp.generated.resources.refresh_icon
import followmymus.composeapp.generated.resources.sync_menu_item
import followmymus.composeapp.generated.resources.upload
import io.github.alexmaryin.followmymus.core.ui.modifiers.animatedShimmerBrush
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.nicknameAvatar.AvatarState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun Avatar(
    state: AvatarState,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onSyncRequest: () -> Unit,
    onImportRequest: () -> Unit = {},
    onExportRequest: () -> Unit = {},
    isImporting: Boolean = false,
    isExporting: Boolean = false
) {
    val avatarColor = remember(state.nickname) { avatarColor(state.nickname) }
    val avatarTextColor = remember { avatarTextColor(avatarColor) }
    var showActions by remember { mutableStateOf(false) }

    BadgedBox(
        badge = { if (state.isSyncing || state.hasPending) Badge() },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(avatarColor)
                .clickable { showActions = !showActions }
                .animatedShimmerBrush(
                    showShimmer = state.isSyncing || isExporting || isImporting,
                    shape = CircleShape,
                    colors = listOf(
                        avatarColor, Color.White, avatarColor
                    )
                )
        ) {
            // Initials
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(6.dp)
            ) {
                Text(
                    text = avatarInitials(state.nickname),
                    autoSize = TextAutoSize.StepBased(),
                    color = avatarTextColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        DropdownMenu(
            expanded = showActions,
            onDismissRequest = { showActions = false },
            containerColor = avatarColor,
            shape = RoundedCornerShape(24.dp)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.sync_menu_item)) },
                onClick = {
                    showActions = false
                    onSyncRequest()
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.refresh_icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                colors = MenuDefaults.itemColors(
                    textColor = avatarTextColor,
                    leadingIconColor = avatarTextColor
                )
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.favorites_import_menu_item)) },
                onClick = {
                    showActions = false
                    onImportRequest()
                },
                enabled = !isImporting,
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.upload),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                colors = MenuDefaults.itemColors(
                    textColor = avatarTextColor,
                    leadingIconColor = avatarTextColor
                )
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.favorites_export_menu_item)) },
                onClick = {
                    showActions = false
                    onExportRequest()
                },
                enabled = !isExporting,
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.download),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                colors = MenuDefaults.itemColors(
                    textColor = avatarTextColor,
                    leadingIconColor = avatarTextColor
                )
            )
        }
    }
}

@Preview
@Composable
fun AvatarPreview() {
    Surface {
        val state = AvatarState(
            nickname = "john.doe",
            hasPending = false,
            isSyncing = false
        )
        Row(
            modifier = Modifier.height(56.dp).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Avatar(state = state, onSyncRequest =  {})
            Avatar(state.copy(hasPending = true), onSyncRequest =  {})
            Avatar(state.copy(isSyncing = true), onSyncRequest =  {})
        }
    }
}
