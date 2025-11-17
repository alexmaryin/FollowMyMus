package io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb

import androidx.room.Embedded
import androidx.room.Relation

data class ArtistWithRelations(
    @Embedded val artist: ArtistEntity,

    @Relation(
        parentColumn = "areaId",
        entityColumn = "id"
    ) val area: AreaEntity?,

    @Relation(
        parentColumn = "beginAreaId",
        entityColumn = "id"
    ) val beginArea: AreaEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "artistId"
    ) val tags: List<TagEntity> = emptyList()
)