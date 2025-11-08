package io.github.alexmaryin.followmymus.core

import java.util.Locale

actual fun changeLanguage(lang: String?) {
    val locale = if (lang != null) Locale.forLanguageTag(lang) else Locale.getDefault()
    Locale.setDefault(locale)
}
