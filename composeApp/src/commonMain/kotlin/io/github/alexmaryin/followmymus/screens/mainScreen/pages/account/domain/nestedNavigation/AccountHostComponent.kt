package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.Page
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.AccountPageState

interface AccountHostComponent : Page {
    val childStack: Value<ChildStack<*, Child>>

    override val state: Value<AccountPageState>
    operator fun invoke(action: AccountAction)

    sealed interface Child {
        data class Account(val nickname: String) : Child
        data object PrivacyPolicy : Child
        data object About : Child
    }
}