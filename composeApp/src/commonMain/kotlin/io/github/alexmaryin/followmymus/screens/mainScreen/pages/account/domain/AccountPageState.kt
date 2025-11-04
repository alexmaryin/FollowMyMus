package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain

import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PageState
import kotlinx.serialization.Serializable

@Serializable
data class AccountPageState(
    val nickname: String = "",
    val language: String = "",
    val theme: String = "",
    val version: String = "",
    val isQrVisible: Boolean = false,
    val backVisible: Boolean = false
) : PageState(backVisible)
