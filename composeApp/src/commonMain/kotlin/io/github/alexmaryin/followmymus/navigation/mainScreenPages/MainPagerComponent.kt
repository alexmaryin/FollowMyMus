package io.github.alexmaryin.followmymus.navigation.mainScreenPages

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select

class MainPagerComponent(
    private val componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val navigation = PagesNavigation<PagerConfig>()

    private val pages = childPages(
        source = navigation,
        serializer = PagerConfig.serializer(),
        initialPages = {
            Pages(
                items = listOf(PagerConfig.Releases, PagerConfig.Favorites),
                selectedIndex = PagerConfig.RELEASES_PAGE
            )
        },
        handleBackButton = true
    ) { page, context ->
        when (page) {
            PagerConfig.Favorites -> {}
            PagerConfig.Releases -> {}
        }
    }

    fun selectPage(index: Int) = navigation.select(index)
}