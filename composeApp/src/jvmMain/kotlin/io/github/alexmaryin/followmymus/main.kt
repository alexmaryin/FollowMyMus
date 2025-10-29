package io.github.alexmaryin.followmymus

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.alexmaryin.followmymus.core.FollowMyMusApp
import io.github.alexmaryin.followmymus.navigation.MainRootComponent
import io.github.alexmaryin.followmymus.navigation.ui.RootContent
import org.koin.ksp.generated.startKoin

fun main() {

    FollowMyMusApp.startKoin {
        printLogger()
        modules()
    }

    val lifecycle = LifecycleRegistry()
    val root = runOnUiThread {
        MainRootComponent(DefaultComponentContext(lifecycle))
    }

    application {
        val windowState = rememberWindowState()
        LifecycleController(lifecycle, windowState)

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "FollowMyMus",
        ) {
            RootContent(root)
        }
    }
}