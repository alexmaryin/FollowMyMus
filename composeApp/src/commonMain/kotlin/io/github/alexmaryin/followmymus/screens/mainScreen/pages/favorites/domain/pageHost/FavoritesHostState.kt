package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost

import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PageState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.nicknameAvatar.AvatarState

data class FavoritesHostState(
    val artistIdSelected: String? = null,
    val releaseIdSelected: String? = null,
    val backVisible: Boolean = false,
    val avatarState: AvatarState,
): PageState(backVisible)
