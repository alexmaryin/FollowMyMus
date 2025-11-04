package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation

import kotlinx.serialization.Serializable

@Serializable
sealed class AccountPageConfig {

    @Serializable
    data object Account : AccountPageConfig()

    @Serializable
    data object PrivacyPolicy : AccountPageConfig()

    @Serializable
    data object About : AccountPageConfig()
}