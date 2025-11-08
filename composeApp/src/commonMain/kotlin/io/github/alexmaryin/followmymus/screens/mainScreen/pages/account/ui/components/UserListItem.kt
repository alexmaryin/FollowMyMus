package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.avatar
import followmymus.composeapp.generated.resources.close_qr
import followmymus.composeapp.generated.resources.logout
import followmymus.composeapp.generated.resources.qr_code
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun UserListItem(
    nickname: String,
    isQrOpened: Boolean,
    onQrToggle: () -> Unit,
    onLogout: () -> Unit
) = ListItem(
    modifier = Modifier.fillMaxWidth(),
    headlineContent = {
        Text(
            text = nickname,
            style = MaterialTheme.typography.titleLarge
        )
    },
    leadingContent = {
        Icon(
            painter = painterResource(Res.drawable.avatar),
            contentDescription = null,
            modifier = Modifier.padding(6.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    },
    trailingContent = {
        Row {
            val iconRes = if (isQrOpened) Res.drawable.close_qr else Res.drawable.qr_code
            val description = if (isQrOpened) "close QR" else "generate QR for login"
            Icon(
                painter = painterResource(iconRes),
                contentDescription = description,
                modifier = Modifier.padding(6.dp)
                    .clickable(onClick = onQrToggle),
                tint = MaterialTheme.colorScheme.primaryFixedDim
            )
            Icon(
                painter = painterResource(Res.drawable.logout),
                contentDescription = "logout",
                modifier = Modifier.padding(6.dp)
                    .clickable(onClick = onLogout),
                tint = MaterialTheme.colorScheme.primaryFixedDim
            )
        }
    },
    colors = ListItemDefaults.colors().copy(containerColor = Color.Transparent)
)

@Preview
@Composable
fun UserListItemPreview() {
    Surface {
        UserListItem(
            "Metallica",
            true,
            {},
            {},
        )
    }
}