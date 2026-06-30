package io.github.alexmaryin.followmymus.preferences

import io.github.alexmaryin.followmymus.core.system.getAppDataDir
import java.io.File

actual fun platformPrefsPath(): String {
    val appDataDir = getAppDataDir()
    if (!appDataDir.exists()) appDataDir.mkdirs()
    return File(appDataDir, PREFS_FILE).absolutePath
}
