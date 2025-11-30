package io.github.alexmaryin.followmymus.screens.mainScreen.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.pages.ChildPages
import com.arkivanov.decompose.extensions.compose.pages.PagesScrollAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.alexmaryin.followmymus.core.ui.ObserveEvents
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenAction
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.MainPages
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PagerComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.AccountPageUi
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation.ArtistsHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.ArtistsPageHostUi
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.FavoritesPageHostUi
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.releases.ui.SearchPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    component: PagerComponent
) {
    val screenPages by component.pages.subscribeAsState()
    val state by component.state.subscribeAsState()
    val currentPage = screenPages.items[state.activePageIndex].instance
    val snackBarHostState = remember { SnackbarHostState() }

    currentPage?.let { page ->
        ObserveEvents(page.snackbarMessages, page.key) {
            snackBarHostState.showSnackbar(it)
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                navigationIcon = { currentPage?.leadingIcon() },
                title = { currentPage?.titleContent() },
                actions = { currentPage?.trailingIcon(this) }
            )
        },
        bottomBar = {
            BottomNavBar(screenPages.selectedIndex) { pageIdx ->
                component(MainScreenAction.SelectPage(pageIdx))
            }
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        floatingActionButton = { currentPage?.fabContent() }
    ) { paddingValues ->

        ChildPages(
            pages = screenPages,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            onPageSelected = {
                component(MainScreenAction.SelectPage(it))
            },
            scrollAnimation = PagesScrollAnimation.Default
        ) { index, page ->

            when (index) {
                MainPages.ARTISTS.index -> ArtistsPageHostUi(page as ArtistsHostComponent)
                MainPages.ACCOUNT.index -> AccountPageUi(page as AccountHostComponent)
                MainPages.RELEASES.index -> SearchPage()
                MainPages.FAVORITES.index -> FavoritesPageHostUi(page as FavoritesHostComponent)
            }
        }
    }
}