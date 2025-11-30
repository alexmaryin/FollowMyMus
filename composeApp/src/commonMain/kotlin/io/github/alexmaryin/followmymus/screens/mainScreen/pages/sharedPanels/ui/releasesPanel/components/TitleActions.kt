package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.refresh_icon
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel.ReleasesListAction
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TitleActions(handler: (ReleasesListAction) -> Unit) {
    ButtonGroup(
        overflowIndicator = { state -> },
    ) {
        clickableItem(
            onClick = { handler(ReleasesListAction.LoadFromRemote) },
            label = "",
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.refresh_icon),
                    contentDescription = "refresh",
                    modifier = Modifier.size(24.dp)
                )
            }
        )
    }
}

@Preview
@Composable
fun TitleActionsPreview() {
    Surface {
        TitleActions(handler = {})
    }
}