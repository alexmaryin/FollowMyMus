package io.github.alexmaryin.followmymus.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.alexmaryin.followmymus.core.system.getAppDataDir
import java.io.File

@Composable
actual fun rememberPrefs(): Prefs {
    val appDataDir = getAppDataDir()
    if (!appDataDir.exists()) appDataDir.mkdirs()
    return remember {
        val prefsFile = File(appDataDir, PREFS_FILE)
        createDataStore {
            prefsFile.absolutePath
        }
    }
}