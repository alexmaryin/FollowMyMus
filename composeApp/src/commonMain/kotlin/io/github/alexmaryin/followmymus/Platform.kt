package io.github.alexmaryin.followmymus

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform