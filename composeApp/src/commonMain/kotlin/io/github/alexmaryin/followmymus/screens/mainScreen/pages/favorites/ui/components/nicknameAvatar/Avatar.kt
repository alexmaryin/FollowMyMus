package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.nicknameAvatar

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.refresh_icon
import followmymus.composeapp.generated.resources.sync_menu_item
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.nicknameAvatar.AvatarState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun Avatar(
    state: AvatarState,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onSyncRequest: () -> Unit
) {
    val avatarColor = remember(state.nickname) { avatarColor(state.nickname) }
    val avatarTextColor = remember { avatarTextColor(avatarColor) }

    val transition = rememberInfiniteTransition()
    val shimmerOffset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1500, easing = LinearEasing)
        ),
        label = "shimmer"
    )

    var showActions by remember { mutableStateOf(false) }

    BadgedBox(
        badge = { if (state.hasPending && !state.isSyncing) Badge() },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(avatarColor)
                .clickable { showActions = !showActions }
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

            if (state.isSyncing) {
                SyncAvatarEffect(shimmerOffset = shimmerOffset)
            }
        }

        DropdownMenu(
            expanded = showActions,
            onDismissRequest = { showActions = false },
            containerColor = avatarColor
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
                colors = MenuDefaults.itemColors(textColor = avatarTextColor)
            )
        }
    }
}

@Preview
@Composable
fun AvatarPreview() {
    Surface {
        val state = AvatarState(
            nickname = "alex_maryin",
            hasPending = false,
            isSyncing = false
        )
        Row(
            modifier = Modifier.height(56.dp).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Avatar(state = state) {}
            Avatar(state.copy(hasPending = true)) {}
            Avatar(state.copy(isSyncing = true)) {}
        }


    }
}
