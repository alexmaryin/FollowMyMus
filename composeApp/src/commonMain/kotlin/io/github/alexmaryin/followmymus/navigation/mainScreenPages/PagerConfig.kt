package io.github.alexmaryin.followmymus.navigation.mainScreenPages

import kotlinx.serialization.Serializable

@Serializable
sealed class PagerConfig {
    @Serializable
    data object Releases : PagerConfig()

    @Serializable
    data object Favorites : PagerConfig()

    companion object {
        const val RELEASES_PAGE = 0
        const val FAVORITES_PAGE = 1
    }
}