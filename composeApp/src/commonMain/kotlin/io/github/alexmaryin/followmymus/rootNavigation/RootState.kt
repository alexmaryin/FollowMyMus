package io.github.alexmaryin.followmymus.rootNavigation

import kotlinx.serialization.Serializable

@Serializable
data class RootState(
    val isDark: Boolean = false,
    val languageTag: String? = null,
    val dynamicMode: Boolean = true
)
