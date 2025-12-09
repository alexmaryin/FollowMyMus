package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel

import androidx.compose.runtime.Composable
import followmymus.composeapp.generated.resources.ISO_unknown
import followmymus.composeapp.generated.resources.Res
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ArtistType
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.CountryISO
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.toLocalizedResourceName
import io.github.alexmaryin.followmymus.screens.utils.DateCategory
import org.jetbrains.compose.resources.stringResource

sealed class SortKeyType(val key: String) {
    data object None : SortKeyType("")
    data class Date(val date: DateCategory) : SortKeyType(date.toString())
    data class Country(val country: CountryISO) : SortKeyType(country.country)
    data class Abc(val letter: String) : SortKeyType(letter)
    data class Type(val value: ArtistType) : SortKeyType(value.name)
}

@Composable
fun SortKeyType.caption() = when (this) {
    is SortKeyType.Abc -> letter

    is SortKeyType.Country -> {
        val resource = country.toLocalizedResourceName()
        stringResource(resource ?: Res.string.ISO_unknown)
    }

    is SortKeyType.Date -> when (date) {
        is DateCategory.ByYear -> date.year.toString()
        is DateCategory.Recent -> stringResource(date.type.titleRes)
    }

    is SortKeyType.Type -> value.name
    else -> ""
}