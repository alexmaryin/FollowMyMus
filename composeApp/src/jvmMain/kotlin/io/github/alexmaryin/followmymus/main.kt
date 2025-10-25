package io.github.alexmaryin.followmymus

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.alexmaryin.followmymus.core.FollowMyMusApp
import org.koin.ksp.generated.startKoin

fun main() {

    FollowMyMusApp.startKoin {
        printLogger()
        modules()
    }

    application {

        Window(
            onCloseRequest = ::exitApplication,
            title = "FollowMyMus",
        ) {
            App()
        }
    }
}