package io.github.alexmaryin.followmymus

import io.github.alexmaryin.followmymus.core.FollowMyMusApp
import org.koin.plugin.module.dsl.startKoin

fun initKoin() {
    startKoin<FollowMyMusApp> {
        printLogger()
    }
}
