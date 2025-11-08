package io.github.alexmaryin.followmymus.preferences

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.language_english
import followmymus.composeapp.generated.resources.language_russian
import followmymus.composeapp.generated.resources.language_system
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

enum class Language(val caption: StringResource) {
    ENGLISH(Res.string.language_english),
    RUSSIAN(Res.string.language_russian),
    SYSTEM(Res.string.language_system);

    companion object {
        suspend fun fromCaption(caption: String) =
            Language.entries.find { getString(it.caption) == caption } ?: SYSTEM
    }
}