package io.github.alexmaryin.followmymus.preferences

import android.content.Context
import org.koin.java.KoinJavaComponent.get

actual fun platformPrefsPath(): String {
    val context: Context = get(Context::class.java)
    return context.filesDir.resolve(PREFS_FILE).absolutePath
}
