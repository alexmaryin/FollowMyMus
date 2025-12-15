package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.more_vert
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import org.jetbrains.compose.resources.painterResource

@Composable
fun ReleasesListTitle(
    modifier: Modifier = Modifier,
    artistName: String,
    actionsHandler: (ReleasesListAction) -> Unit
) {
    var isActionsVisible by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { isActionsVisible = !isActionsVisible },
        ) {
            Icon(
                painter = painterResource(Res.drawable.more_vert),
                contentDescription = "actions for $artistName"
            )
        }
        AnimatedVisibility( isActionsVisible) {
            TitleActions { actionsHandler(it); isActionsVisible = false }
        }
        Text(
            artistName,
            style = MaterialTheme.typography.titleLarge,
            modifier = modifier.padding(4.dp)
        )
    }
}