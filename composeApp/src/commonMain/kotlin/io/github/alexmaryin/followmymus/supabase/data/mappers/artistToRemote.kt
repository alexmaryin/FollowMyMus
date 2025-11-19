package io.github.alexmaryin.followmymus.supabase.data.mappers

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.supabase.domain.model.ArtistRemoteEntity

fun Artist.toRemote() = ArtistRemoteEntity(artistId = id)