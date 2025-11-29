package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.ReleaseType
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.SecondaryType
import kotlinx.datetime.LocalDate

data class Release(
    val id: String,
    val title: String,
    val disambiguation: String?,
    val firstReleaseDate: LocalDate?,
    val primaryType: ReleaseType,
    val secondaryTypes: List<SecondaryType>,
    val previewCoverUrl: String?,
    val largeCoverUrl: String?
)
