package io.github.alexmaryin.followmymus.screens.mainScreen.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.MainPages
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomNavBar(
    selectedPage: Int,
    onPageClick: (index: Int) -> Unit
) = NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
    MainPages.entries.forEach { page ->
        NavigationBarItem(
            selected = page.index == selectedPage,
            onClick = { onPageClick(page.index) },
            icon = {
                Icon(
                    painter = painterResource(
                        if (page.index == selectedPage)
                            page.iconActiveRes else page.iconRes
                    ),
                    contentDescription = page.name
                )
            },
            label = { Text(stringResource(page.titleRes)) }
        )
    }
}
