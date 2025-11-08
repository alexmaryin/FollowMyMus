package io.github.alexmaryin.followmymus.preferences

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.theme_dark
import followmymus.composeapp.generated.resources.theme_light
import followmymus.composeapp.generated.resources.theme_system
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

enum class ThemeMode(val caption: StringResource) {
    LIGHT(Res.string.theme_light),
    DARK(Res.string.theme_dark),
    SYSTEM(Res.string.theme_system);

    companion object {
        suspend fun fromCaption(caption: String) =
            ThemeMode.entries.find { getString(it.caption) == caption } ?: SYSTEM
    }
}