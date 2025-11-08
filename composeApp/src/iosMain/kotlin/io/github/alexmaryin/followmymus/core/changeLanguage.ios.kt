package io.github.alexmaryin.followmymus.core

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual fun changeLanguage(lang: String?) {
    val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
    println(settingsUrl)
    settingsUrl?.let { url ->
        UIApplication.sharedApplication.openURL(
            url = url,
            options = emptyMap<Any?, Any?>(),
            completionHandler = null
        )
    }
}