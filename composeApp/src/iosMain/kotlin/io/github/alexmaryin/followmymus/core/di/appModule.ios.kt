package io.github.alexmaryin.followmymus.core.di

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

actual fun getHttpEngine(): HttpClientEngine = Darwin.create()