package io.github.alexmaryin.followmymus.supabase.domain

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.supabase.domain.model.ArtistRemoteEntity

interface SupabaseDb {
    suspend fun getRemoteFavoritesArtists(): Result<List<ArtistRemoteEntity>>
    suspend fun addRemoteFavoriteArtist(artist: ArtistRemoteEntity): Result<Unit>
    suspend fun removeRemoteFavoriteArtist(artistId: String): Result<Unit>

    companion object {
        const val SUPABASE_NAME = "favorite_artists"
    }
}