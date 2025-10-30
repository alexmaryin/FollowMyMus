package io.github.alexmaryin.followmymus.navigation.mainScreenPages

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select

class MainPagerComponent(
    private val componentContext: ComponentContext,
    val nickName: String
) : ComponentContext by componentContext {

    private val navigation = PagesNavigation<PagerConfig>()

    val pages = childPages(
        source = navigation,
        serializer = PagerConfig.serializer(),
        initialPages = {
            Pages(
                items = listOf(PagerConfig.Releases, PagerConfig.Favorites),
                selectedIndex = MainPages.RELEASES.index
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