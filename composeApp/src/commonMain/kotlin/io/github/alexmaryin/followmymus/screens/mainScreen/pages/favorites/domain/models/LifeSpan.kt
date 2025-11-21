package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class LifeSpan(
    val begin: String?,
    val end: String?,
    val ended: Boolean?
)
