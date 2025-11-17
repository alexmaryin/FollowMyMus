package io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.allStringResources
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class CountryISO (val country: String)

fun CountryISO.toLocalizedResourceName() = Res.allStringResources["ISO_$country"]
