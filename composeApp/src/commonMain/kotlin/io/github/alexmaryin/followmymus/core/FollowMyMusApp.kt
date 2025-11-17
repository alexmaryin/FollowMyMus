package io.github.alexmaryin.followmymus.core

import io.github.alexmaryin.followmymus.core.di.AppModule
import io.github.alexmaryin.followmymus.core.di.DbModule
import org.koin.core.annotation.KoinApplication

@KoinApplication(
    modules = [AppModule::class, DbModule::class]
)
object FollowMyMusApp