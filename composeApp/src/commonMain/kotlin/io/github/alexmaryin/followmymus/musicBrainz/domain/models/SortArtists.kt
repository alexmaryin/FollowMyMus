package io.github.alexmaryin.followmymus.musicBrainz.domain.models

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.sort_by_abc
import followmymus.composeapp.generated.resources.sort_by_country
import followmymus.composeapp.generated.resources.sort_by_date
import followmymus.composeapp.generated.resources.sort_by_none
import followmymus.composeapp.generated.resources.sort_by_type
import org.jetbrains.compose.resources.StringResource

enum class SortArtists(val title: StringResource) {
    NONE(Res.string.sort_by_none),
    DATE(Res.string.sort_by_date),
    COUNTRY(Res.string.sort_by_country),
    ABC(Res.string.sort_by_abc),
    TYPE(Res.string.sort_by_type)
}