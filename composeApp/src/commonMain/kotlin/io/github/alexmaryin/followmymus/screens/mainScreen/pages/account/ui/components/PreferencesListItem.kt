package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.models.PreferencesItem
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.models.TrailingIconType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
fun PreferenceListItem(item: PreferencesItem) {
    ListItem(
        headlineContent = { Text(item.text, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            Icon(
                painter = painterResource(item.leadingIconRes),
                contentDescription = null
            )
        },
        trailingContent = {
            Row {
                item.trailingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primaryFixedDim
                    )
                }
                val (iconRes, description) = when (item.type) {
                    TrailingIconType.FORWARD -> Res.drawable.forward to "show more"
                    TrailingIconType.SELECT -> Res.drawable.unfold_more to "change"
                }
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = description,
                    tint = MaterialTheme.colorScheme.primaryFixedDim,
                    modifier = Modifier.clickable(onClick = item.onClick)
                )
            }
        },
        colors = ListItemDefaults.colors().copy(containerColor = Color.Transparent)
    )
    if (item.withDivider) HorizontalDivider(
        modifier = Modifier.padding(horizontal = 28.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.secondaryFixed
    )
}

@Preview
@Composable
fun PreferenceListItemPreview() {
    Surface {
        Column {
            PreferenceListItem(
                PreferencesItem(
                    text = "Language",
                    leadingIconRes = Res.drawable.language,
                    trailingText = "English",
                    withDivider = true,
                    onClick = {}
                )
            )
            PreferenceListItem(
                PreferencesItem(
                    text = "theme",
                    leadingIconRes = Res.drawable.theme,
                    trailingText = "System",
                    withDivider = true,
                    onClick = {}
                )
            )
        }
    }
}

