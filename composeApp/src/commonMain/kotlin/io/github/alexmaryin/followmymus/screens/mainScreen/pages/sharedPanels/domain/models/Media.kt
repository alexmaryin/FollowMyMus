package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.CountryISO
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.ReleaseStatus
import kotlinx.datetime.LocalDate

data class Media(
    val id: String,
    val title: String,
    val disambiguation: String?,
    val status: ReleaseStatus,
    val country: CountryISO,
    val date: LocalDate?,
    val barcode: String?,
    val quality: String?,
    val items: List<MediaItem> = emptyList(),
    val resources: List<Resource> = emptyList(),
    val previewCoverUrl: String? = null,
    val fullCoverUrl: String? = null
)
