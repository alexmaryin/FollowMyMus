package io.github.alexmaryin.followmymus.core.di

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

actual fun getHttpEngine(): HttpClientEngine = OkHttp.create()