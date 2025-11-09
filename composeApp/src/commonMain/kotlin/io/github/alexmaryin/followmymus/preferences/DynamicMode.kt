package io.github.alexmaryin.followmymus.preferences

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.dynamic_off
import followmymus.composeapp.generated.resources.dynamic_on
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

enum class DynamicMode(val caption: StringResource) {
    ON(Res.string.dynamic_on), OFF(Res.string.dynamic_off);

    companion object {
        suspend fun fromCaption(caption: String) =
            DynamicMode.entries.find { getString(it.caption) == caption } ?: ON
    }
}