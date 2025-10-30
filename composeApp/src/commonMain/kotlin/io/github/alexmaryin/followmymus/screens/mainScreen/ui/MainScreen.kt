package io.github.alexmaryin.followmymus.screens.mainScreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arkivanov.decompose.extensions.compose.pages.ChildPages
import com.arkivanov.decompose.extensions.compose.pages.PagesScrollAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.app_name
import io.github.alexmaryin.followmymus.navigation.mainScreenPages.MainPagerComponent
import io.github.alexmaryin.followmymus.navigation.mainScreenPages.MainPages
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    component: MainPagerComponent
) {
    val scope = rememberCoroutineScope()
    var activePage by remember { mutableStateOf(MainPages.RELEASES.index) }

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.app_name))
                },
                navigationIcon = {

                }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                MainPages.entries.forEach { page ->
                    NavigationBarItem(
                        selected = page.index == activePage,
                        onClick = { component.selectPage(page.index) },
                        icon = {
                            Icon(
                                painter = painterResource(page.iconRes),
                                contentDescription = page.name
                            )
                        },
                        label = { Text(stringResource(page.titleRes)) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavigationRailContent(
            modifier = Modifier.padding(paddingValues),
            nickname = component.nickName,
            onSettingsClick = { /*TODO*/ },
            onSignOutClick = { /*TODO*/ }
        )

        Box(modifier = Modifier.padding(paddingValues)) {
            ChildPages(
                pages = component.pages,
                modifier = Modifier.fillMaxSize(),
                onPageSelected = { component.selectPage(it) },
                scrollAnimation = PagesScrollAnimation.Default
            ) { page, childComponent ->
                activePage = page
                Box(modifier = Modifier.fillMaxSize().background(color = Color(Random.nextLong()))) {
                    Text(
                        text = "PAGE #$page",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}