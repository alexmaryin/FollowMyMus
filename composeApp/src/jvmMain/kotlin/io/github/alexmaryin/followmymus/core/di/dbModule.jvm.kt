package io.github.alexmaryin.followmymus.core.di

import io.github.alexmaryin.followmymus.musicBrainz.data.local.db.MusicBrainzDbFactory

actual fun getDbMusicBrainzDbFactory() = MusicBrainzDbFactory()