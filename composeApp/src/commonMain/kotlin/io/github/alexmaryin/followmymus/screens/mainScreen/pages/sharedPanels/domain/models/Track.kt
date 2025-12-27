package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models

data class Track(
    val id: String,
    val position: Int,
    val title: String,
    val lengthMs: Long?,
    val disambiguation: String?
)
