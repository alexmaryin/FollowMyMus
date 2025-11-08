package io.github.alexmaryin.followmymus.core

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun changeLanguage(lang: String?) {
    // iOS does not allow changing the app's locale programmatically.
    // The language is determined by the device's system settings.
    // This function can be used to get the current language.
    val currentLanguage = NSLocale.currentLocale.languageCode
    println("Current device language: $currentLanguage")
}