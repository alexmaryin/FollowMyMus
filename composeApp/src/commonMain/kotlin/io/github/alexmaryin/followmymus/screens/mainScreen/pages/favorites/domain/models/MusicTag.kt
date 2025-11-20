package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class MusicTag(val name: String)