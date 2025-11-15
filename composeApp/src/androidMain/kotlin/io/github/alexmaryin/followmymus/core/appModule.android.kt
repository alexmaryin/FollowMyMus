package io.github.alexmaryin.followmymus.core

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

actual fun getHttpEngine(): HttpClientEngine = OkHttp.create()