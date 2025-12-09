package io.github.alexmaryin.followmymus.musicBrainz.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AreaEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    val name: String,
    val sortName: String,
    @Embedded(prefix = "area_lifespan_")
    val lifeSpan: LifeSpanEntity?
)
