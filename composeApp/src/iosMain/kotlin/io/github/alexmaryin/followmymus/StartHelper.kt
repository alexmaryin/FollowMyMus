package io.github.alexmaryin.followmymus

import io.github.alexmaryin.followmymus.core.FollowMyMusApp
import io.kotzilla.sdk.analytics.koin.analytics
import org.koin.ksp.generated.startKoin

fun initKoin() {
    FollowMyMusApp.startKoin {
        printLogger()
        modules()
        analytics()
    }
}