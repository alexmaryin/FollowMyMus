package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.ErrorType

sealed class FavoritesImportExportError : ErrorType() {
    data class DataReadError(val cause: String?) : FavoritesImportExportError()
    data object Malformed : FavoritesImportExportError()
    data object UnsupportedFormat : FavoritesImportExportError()
    data class UnsupportedVersion(val version: Int) : FavoritesImportExportError()
    data object MissingArtistsField : FavoritesImportExportError()
    data class EmptyArtistEntry(val index: Int) : FavoritesImportExportError()
    data class NetworkError(val cause: String?) : FavoritesImportExportError()
}
