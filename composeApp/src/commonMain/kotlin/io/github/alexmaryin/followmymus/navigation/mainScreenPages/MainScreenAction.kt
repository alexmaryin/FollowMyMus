package io.github.alexmaryin.followmymus.navigation.mainScreenPages

sealed class MainScreenAction {

    data class SelectPage(val index: Int) : MainScreenAction()
}