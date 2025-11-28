package io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SecondaryType {
    @SerialName("Audio Drama")
    AUDIO_DRAMA,

    @SerialName("Audiobook")
    AUDIOBOOK,

    @SerialName("Compilation")
    COMPILATION,

    @SerialName("Demo")
    DEMO,

    @SerialName("Dj-Mix")
    DJ_MIX,

    @SerialName("Field Recording")
    FIELD_RECORDING,

    @SerialName("Interview")
    INTERVIEW,

    @SerialName("Live")
    LIVE,

    @SerialName("Mixtape/Street")
    MIXTAPE_STREET,

    @SerialName("Remix")
    REMIX,

    @SerialName("Soundtrack")
    SOUNDTRACK,

    @SerialName("Spokenword")
    SPOKENWORD
}
