package io.github.alexmaryin.followmymus.screens.mainScreen.domain

import androidx.compose.runtime.Composable

interface HasSearchBar {
    val searchIsVisible: Boolean
    @Composable fun ProvideSearchBar()
}