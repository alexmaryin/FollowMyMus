package io.github.alexmaryin.followmymus.supabase.data.mappers

import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ArtistDto
import io.github.alexmaryin.followmymus.supabase.domain.model.ArtistRemoteEntity

fun ArtistDto.toRemote() = ArtistRemoteEntity(artistId = id)