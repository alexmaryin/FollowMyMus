package io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenAction
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
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
                items = listOf(PagerConfig.Releases, PagerConfig.Favorites, PagerConfig.Account),
                selectedIndex = MainPages.RELEASES.index
            )
        },
        handleBackButton = true
    ) { page, context ->
        when (page) {
            PagerConfig.Favorites -> DummyPage
            PagerConfig.Releases -> DummyPage
            is PagerConfig.Account -> get<AccountHostComponent> {
                parametersOf(context, state.value.nickname)
            }
        }
    }

    override fun invoke(action: MainScreenAction) {
        when (action) {
            is MainScreenAction.SelectPage -> {
                navigation.select(action.index)
                _state.update { it.copy(activePageIndex = action.index) }
            }

            is MainScreenAction.SetBackIconState -> {
                _state.update { it.copy(backIconVisible = action.isVisible) }
            }

            MainScreenAction.BackClick -> {
                pages.value.items[state.value.activePageIndex].instance?.invoke(PageAction.Back)
            }
        }
    }
}

//TODO delete after
object DummyPage : Page {
    override val state: Value<PageState> = MutableValue(PageState())
    override fun invoke(action: PageAction) = Unit
}