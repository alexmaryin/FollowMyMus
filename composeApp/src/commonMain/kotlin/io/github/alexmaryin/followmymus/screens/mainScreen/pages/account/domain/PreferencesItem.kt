package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain

import org.jetbrains.compose.resources.DrawableResource

data class PreferencesItem(
    val text: String,
    val leadingIconRes: DrawableResource,
    val type: TrailingIconType = TrailingIconType.SELECT,
    val trailingText: String? = null,
    val withDivider: Boolean = false,
    val onClick: () -> Unit
)

enum class TrailingIconType {
    FORWARD, SELECT
}
