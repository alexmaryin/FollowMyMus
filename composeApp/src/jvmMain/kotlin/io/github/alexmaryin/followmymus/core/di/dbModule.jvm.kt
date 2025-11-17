package io.github.alexmaryin.followmymus.core.di

import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.MusicBrainzDbFactory

actual fun getDbMusicBrainzDbFactory() = MusicBrainzDbFactory()