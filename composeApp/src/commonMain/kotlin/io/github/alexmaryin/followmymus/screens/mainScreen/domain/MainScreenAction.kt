package io.github.alexmaryin.followmymus.screens.mainScreen.domain

sealed class MainScreenAction {

    data class SelectPage(val index: Int) : MainScreenAction()
}