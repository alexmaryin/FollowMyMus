package io.github.alexmaryin.followmymus.core.system

import java.io.File

fun getAppDataDir(): File {
    val os = System.getProperty("os.name")?.lowercase() ?: ""
    val userHome = System.getProperty("user.home")
    return when {
        os.contains("win") -> File(System.getenv("APPDATA"), "FollowMyMus")
        os.contains("mac") -> File(userHome, "Library/Application Support/FollowMyMus")
        else -> File(userHome, ".local/share/FollowMyMus")
    }
}