package io.github.alexmaryin.followmymus.core

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

actual fun changeLanguage(lang: String?) {
    val appLocale = if (lang != null) {
        LocaleListCompat.forLanguageTags(lang)
    } else {
        LocaleListCompat.getEmptyLocaleList()
    }
    AppCompatDelegate.setApplicationLocales(appLocale)
}