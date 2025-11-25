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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
            // TODO delete after debug
            println(it)
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = { currentPage?.titleContent() },
                navigationIcon = { currentPage?.leadingIcon() }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                MainPages.entries.forEach { page ->
                    NavigationBarItem(
                        selected = page.index == screenPages.selectedIndex,
                        onClick = { component(MainScreenAction.SelectPage(page.index)) },
                        icon = {
                            Icon(
                                painter = painterResource(
                                    if (page.index == screenPages.selectedIndex)
                                        page.iconActiveRes else page.iconRes
                                ),
                                contentDescription = page.name
                            )
                        },
                        label = { Text(stringResource(page.titleRes)) }
                    )
                }
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