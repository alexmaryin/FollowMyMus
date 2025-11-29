package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.arrow_right
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Resource
import io.ktor.http.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ResourcesFlow(resources: Map<String, List<Resource>>) {
    if (resources.isEmpty()) return

    val resourcesState = rememberLazyStaggeredGridState()
    val uriHandler = LocalUriHandler.current

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) =
                Offset(available.x, 0f)
        }
    }

    LazyHorizontalStaggeredGrid(
        rows = StaggeredGridCells.Adaptive(minSize = 22.dp),
        state = resourcesState,
        modifier = Modifier.sizeIn(maxHeight = 100.dp).nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalItemSpacing = 4.dp,
    ) {
        items(resources.keys.toList()) { name ->
            val item = resources[name]
            requireNotNull(item)

            ResourceChip(
                name = name,
                resources = item,
                uriHandler = uriHandler
            )
        }
    }
}

@Composable
private fun ResourceChip(
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

@OptIn(ExperimentalUuidApi::class)
@Preview
@Composable
fun ResourcesFlowPreview() {
    Surface {
        val resources = listOf(
            Resource("wiki", "https://wiki.com/parts"),
            Resource("allmusic", "https://allmusic.com/parts"),
            Resource("yandex", "https://yandex.com/music"),
            Resource("spotify", "https://spotify.com/music"),
            Resource("google", "https://google.com/music"),
            Resource("100rates", "https://100rates.com/music"),
            Resource("fanpage", "https://fanpage.com/music"),
            Resource("fanpage", "https://fanpage2.com/music"),
            Resource("blog", "https://blog.com/music"),
            Resource("other", "https://other.com/music"),
            Resource("other", "https://other2.com/music"),
            Resource("other", "https://other3.com/music"),
            Resource("other", "https://other4.com/music"),
            Resource("other", "https://other5.com/music"),
            Resource("other", "https://other6.com/music"),
        )
        ResourcesFlow(resources.groupBy { it.resourceName })
    }
}