package io.github.alexmaryin.followmymus.core

import java.util.Locale

object LocaleManager {
    @Suppress("ConstantLocale")
    val systemLocale: Locale? = Locale.getDefault()

    fun changeLanguage(lang: String?) {
        val locale = if (lang != null) Locale.forLanguageTag(lang) else systemLocale
        Locale.setDefault(locale)
    }
}

actual fun changeLanguage(lang: String?) = LocaleManager.changeLanguage(lang)