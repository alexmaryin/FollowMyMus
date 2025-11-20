package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.nicknameAvatar

data class AvatarState(
    val nickname: String,
    val hasPending: Boolean = false,
    val isSyncing: Boolean = false
)
