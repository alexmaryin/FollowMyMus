package io.github.alexmaryin.followmymus.screens.mainScreen.domain

import androidx.compose.runtime.Composable

interface HasTitleBar {
    val contentIsVisible: Boolean
    val content: @Composable () -> Unit
}