package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.nicknameAvatar

import kotlinx.serialization.Serializable

@Serializable
data class AvatarState(
    val nickname: String = "",
    val hasPending: Boolean = false,
    val isSyncing: Boolean = false
)
