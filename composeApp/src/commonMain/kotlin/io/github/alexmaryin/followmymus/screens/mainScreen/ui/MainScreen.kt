package io.github.alexmaryin.followmymus.screens.mainScreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.arkivanov.decompose.extensions.compose.pages.ChildPages
import com.arkivanov.decompose.extensions.compose.pages.PagesScrollAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.app_name
import io.github.alexmaryin.followmymus.BuildKonfig
import io.github.alexmaryin.followmymus.navigation.mainScreenPager.MainPages
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenAction
import io.github.alexmaryin.followmymus.navigation.mainScreenPager.PagerComponent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    component: PagerComponent
) {
    val scope = rememberCoroutineScope()
    val activePage by component.pages.subscribeAsState()
    val state by component.state.subscribeAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.app_name) + " ver.${BuildKonfig.appVersion}")
                },
                navigationIcon = {

                }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                MainPages.entries.forEach { page ->
                    NavigationBarItem(
                        selected = page.index == activePage.selectedIndex,
                        onClick = { component.onAction(MainScreenAction.SelectPage(page.index)) },
                        icon = {
                            Icon(
                                painter = painterResource(
                                    if (page.index == activePage.selectedIndex)
                                        page.iconActiveRes else page.iconRes
                                ),
                                contentDescription = page.name
                            )
                        },
                        label = { Text(stringResource(page.titleRes)) }
                    )
                }
            }
        }
    ) { paddingValues ->

        ChildPages(
            pages = component.pages,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            onPageSelected = { component.onAction(MainScreenAction.SelectPage(it)) },
            scrollAnimation = PagesScrollAnimation.Default
        ) { page, childComponent ->
            Box(modifier = Modifier.fillMaxSize().background(color = Color(Random.nextLong()))) {
                Text(
                    text = "Hello, ${state.nickname}!\nThis is PAGE #$page",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}