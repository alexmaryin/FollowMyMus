package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Resource
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ResourcesColumnFlow(
    modifier: Modifier = Modifier,
    resources: Map<String, List<Resource>>
) {
    if (resources.isEmpty()) return

    val resourcesState = rememberLazyStaggeredGridState()
    val uriHandler = LocalUriHandler.current

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) =
                Offset(0f, available.y)
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(1),
        state = resourcesState,
        modifier = modifier.nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(4.dp),
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

@OptIn(ExperimentalUuidApi::class)
@Preview
@Composable
fun ResourcesColumnFlowPreview() {
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
        ResourcesColumnFlow(resources = resources.groupBy { it.resourceName })
    }
}