package io.github.alexmaryin.followmymus.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PullToRefreshMobile(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    when  {
        isDesktop() && isRefreshing ->
            Box(Modifier.fillMaxSize()) {
                VinylLoadingIndicator(Modifier.align(Alignment.Center).size(100.dp))
            }

        isDesktop() && !isRefreshing -> Box(modifier) { content() }

        else -> PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = pullToRefreshState,
            onRefresh = onRefresh,
            modifier = modifier,
            indicator = {
                RefreshVinylIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            content = content
        )
    }
}