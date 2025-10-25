package io.github.alexmaryin.followmymus

import io.github.alexmaryin.followmymus.core.FollowMyMusApp
import org.koin.ksp.generated.startKoin

fun initKoin() {
    FollowMyMusApp.startKoin {
        printLogger()
        modules()
    }
}