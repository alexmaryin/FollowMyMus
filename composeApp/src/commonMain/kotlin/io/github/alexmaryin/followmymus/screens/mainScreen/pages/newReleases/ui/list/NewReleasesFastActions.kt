package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui.list

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.last_month
import followmymus.composeapp.generated.resources.restore_all_dismissed
import followmymus.composeapp.generated.resources.restore_month
import followmymus.composeapp.generated.resources.revert
import followmymus.composeapp.generated.resources.undo
import followmymus.composeapp.generated.resources.undo_last_dismissal
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.NewReleasesListAction
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NewReleasesActions(
    expanded: Boolean,
    hasDismissals: Boolean,
    onDismiss: () -> Unit,
    onAction: (NewReleasesListAction) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.undo_last_dismissal)) },
            onClick = {
                onDismiss()
                onAction(NewReleasesListAction.UndoLastDismissal)
            },
            enabled = hasDismissals,
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.undo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.restore_all_dismissed)) },
            onClick = {
                onDismiss()
                onAction(NewReleasesListAction.RestoreAllDismissed)
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.revert),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.last_month)) },
            onClick = {
                onDismiss()
                onAction(NewReleasesListAction.RestoreLastMonth)
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.restore_month),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
        )
    }
}
