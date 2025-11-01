package io.github.alexmaryin.followmymus.navigation.mainScreenPager

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenAction
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenState
import org.koin.core.annotation.Factory

@Factory(binds = [PagerComponent::class])
class MainPagerComponent(
    private val componentContext: ComponentContext,
    nickName: String
) : PagerComponent, ComponentContext by componentContext {

    private val navigation = PagesNavigation<PagerConfig>()
    private val _state = MutableValue(MainScreenState(nickName))
    override val state: Value<MainScreenState> get() = _state

    override val pages = childPages(
        source = navigation,
        serializer = PagerConfig.serializer(),
        initialPages = {
            Pages(
                items = listOf(PagerConfig.Releases, PagerConfig.Favorites, PagerConfig.Account),
                selectedIndex = MainPages.RELEASES.index
            )
        },
        handleBackButton = true
    ) { page, context ->
        when (page) {
            PagerConfig.Favorites -> object : Page {}
            PagerConfig.Releases -> object : Page {}
            PagerConfig.Account -> object : Page {}
        }
    }

    override fun onAction(action: MainScreenAction) {
        when (action) {
            is MainScreenAction.SelectPage -> navigation.select(action.index)
        }

    }
}