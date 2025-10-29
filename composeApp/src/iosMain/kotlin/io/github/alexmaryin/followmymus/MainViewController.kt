package io.github.alexmaryin.followmymus

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.alexmaryin.followmymus.navigation.MainRootComponent
import io.github.alexmaryin.followmymus.navigation.ui.RootContent

fun MainViewController() = ComposeUIViewController {
    val lifecycle = LifecycleRegistry()
    val root = MainRootComponent(DefaultComponentContext(lifecycle))
    RootContent(root)
}