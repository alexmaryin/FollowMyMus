package io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api

import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ReleaseGroupDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchReleaseGroupResponse(
    val count: Int,
    val offset: Int = 0,
    @SerialName("release-groups") val releaseGroups: List<ReleaseGroupDto> = emptyList(),
)
