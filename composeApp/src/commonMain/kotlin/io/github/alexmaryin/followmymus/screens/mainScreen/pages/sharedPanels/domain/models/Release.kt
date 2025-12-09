package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ReleaseType
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SecondaryType
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
