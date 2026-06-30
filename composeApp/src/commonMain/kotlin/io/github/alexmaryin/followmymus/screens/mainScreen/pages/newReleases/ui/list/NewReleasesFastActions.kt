package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.revert
import followmymus.composeapp.generated.resources.tune
import followmymus.composeapp.generated.resources.undo
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.NewReleasesListAction
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewReleasesFastActions(
    hasDismissals: Boolean,
    onAction: (NewReleasesListAction) -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }

    IconButton(onClick = { isVisible = !isVisible }) {
        Icon(
            painter = painterResource(Res.drawable.tune),
            contentDescription = "release actions",
        )
    }

    AnimatedVisibility(visible = isVisible) {
        ButtonGroup(
            overflowIndicator = {},
        ) {
            clickableItem(
                onClick = {
                    onAction(NewReleasesListAction.UndoLastDismissal)
                    isVisible = false
                },
                label = "",
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.undo),
                        contentDescription = "undo last dismissal",
                        modifier = Modifier.size(24.dp),
                    )
                },
                enabled = hasDismissals,
            )
            clickableItem(
                onClick = {
                    onAction(NewReleasesListAction.RestoreAllDismissed)
                    isVisible = false
                },
                label = "",
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.revert),
                        contentDescription = "restore all dismissed",
                        modifier = Modifier.size(24.dp),
                    )
                }
            )
        }
    }
}
