package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AccountPageConfig {

    @Serializable
    data class Account(val nickname: String) : AccountPageConfig

    @Serializable
    data object PrivacyPolicy : AccountPageConfig

    @Serializable
    data object About : AccountPageConfig
}