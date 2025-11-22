package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.favoritesPanel.listItem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.arrow_right
import org.jetbrains.compose.resources.painterResource

@Composable
fun DetailsOpenIcon(enabled: Boolean, onClick: (Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 90f else 0f)

    IconButton(
        onClick = {
            expanded = !expanded
            onClick(expanded)
        },
        enabled = enabled
    ) {
        Icon(
            painter = painterResource(Res.drawable.arrow_right),
            contentDescription = if (expanded) "collapse artist" else "expand artist",
            modifier = Modifier.rotate(rotation)
        )
    }
}