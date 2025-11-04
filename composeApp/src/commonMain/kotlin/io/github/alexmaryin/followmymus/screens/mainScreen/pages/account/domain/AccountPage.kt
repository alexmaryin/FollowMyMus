package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import io.github.alexmaryin.followmymus.rootNavigation.ui.saveableMutableValue
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.AccountAction
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PageAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountPageConfig
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent

@Factory(binds = [AccountHostComponent::class])
class AccountPage(
    private val componentContext: ComponentContext
) : Page, AccountHostComponent, ComponentContext by componentContext, KoinComponent {

    private val _state by saveableMutableValue(AccountPageState.serializer(), init = ::AccountPageState)
    override val state get() = _state
    private val navigation = StackNavigation<AccountPageConfig>()

    override val childStack: Value<ChildStack<*, AccountHostComponent.Child>> = childStack(
        source = navigation,
        serializer = AccountPageConfig.serializer(),
        initialConfiguration = AccountPageConfig.Account,
        handleBackButton = true
    ) { config, _ ->
        when (config) {
            AccountPageConfig.Account -> AccountHostComponent.Child.Account
            AccountPageConfig.About -> AccountHostComponent.Child.About
            AccountPageConfig.PrivacyPolicy -> AccountHostComponent.Child.PrivacyPolicy
        }
    }

    override fun invoke(action: PageAction) {
        when (action) {
            PageAction.Back -> {
                _state.update { it.copy(backVisible = false) }
                navigation.bringToFront(AccountPageConfig.Account)
            }

            AccountAction.ShowAbout -> {
                _state.update { it.copy(backVisible = true) }
                navigation.bringToFront(AccountPageConfig.About)
            }

            AccountAction.ShowPrivacyPolicy -> {
                _state.update { it.copy(backVisible = true) }
                navigation.bringToFront(AccountPageConfig.PrivacyPolicy)
            }
        }
    }
}