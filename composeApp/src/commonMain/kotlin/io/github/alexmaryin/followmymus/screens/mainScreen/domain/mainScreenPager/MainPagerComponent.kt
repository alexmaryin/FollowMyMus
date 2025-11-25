package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenAction
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation.ArtistsHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesHostComponent
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

@Factory(binds = [PagerComponent::class])
class MainPagerComponent(
    private val componentContext: ComponentContext,
    nickName: String
) : PagerComponent, ComponentContext by componentContext, KoinComponent {

    private val navigation = PagesNavigation<PagerConfig>()
    private val _state = MutableValue(MainScreenState(nickName))
    override val state: Value<MainScreenState> get() = _state

    override val pages = childPages(
        source = navigation,
        serializer = PagerConfig.serializer(),
        initialPages = {
            Pages(
                items = listOf(
                    PagerConfig.Artists,
                    PagerConfig.Releases,
                    PagerConfig.Favorites,
                    PagerConfig.Account
                ),
                selectedIndex = MainPages.ARTISTS.index
            )
        },
        handleBackButton = true
    ) { page, context ->
        when (page) {
            PagerConfig.Artists -> get<ArtistsHostComponent> { parametersOf(context) }
            PagerConfig.Favorites -> get<FavoritesHostComponent> {
                parametersOf(context, state.value.nickname)
            }
            PagerConfig.Releases -> DummyPage
            is PagerConfig.Account -> get<AccountHostComponent> {
                parametersOf(context, state.value.nickname)
            }
        }
    }

    override fun invoke(action: MainScreenAction) {
        when (action) {
            is MainScreenAction.SelectPage -> {
                if (action.index != state.value.activePageIndex) {
                    _state.update { it.copy(activePageIndex = action.index) }
                    navigation.select(action.index)
                }
            }
        }
    }
}

//TODO delete after implementation all pages
object DummyPage : Page, ScaffoldSlots by DefaultScaffoldSlots {
    override val key get() = "Dummy"
}