package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.arrow_right
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Resource
import io.ktor.http.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun ResourceChip(
    name: String,
    resources: List<Resource>,
    uriHandler: UriHandler
) {
    var expanded by remember { mutableStateOf(false) }
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "iconRotation"
    )

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopStart)
    ) {
        if (resources.size == 1) {
            AssistChip(
                onClick = { uriHandler.openUri(resources.first().url) },
                label = { Text(text = name) }
            )
        } else {
            AssistChip(
                onClick = { expanded = true },
                trailingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_right),
                        contentDescription = "open links for $name",
                        modifier = Modifier.rotate(iconRotation)
                    )
                },
                label = { Text(text = name) }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(28.dp)
            ) {
                resources.forEach { resource ->
                    DropdownMenuItem(
                        text = { Text(text = Url(resource.url).host) },
                        onClick = {
                            uriHandler.openUri(resource.url)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}