package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.core.requireSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.ReleaseDto
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toArtistReleases
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.ArtistReleases
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.collections.AtomicMutableList
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [ReleasesRepository::class])
class ApiReleasesRepository(
    private val searchEngine: SearchEngine
) : ReleasesRepository {
    @OptIn(InternalAPI::class, SupabaseInternal::class)
    override fun searchArtistReleases(artistId: String): Flow<ArtistReleases> = channelFlow {
        val releasesResult = searchEngine.searchReleases(artistId)
        releasesResult.forSuccess { artistDto ->
            send(artistDto.toArtistReleases())
        }
        releasesResult.forError {
            return@channelFlow
        }

        val artistDto = releasesResult.requireSuccess()
        val releases = AtomicMutableList<ReleaseDto>()
        releases.addAll(artistDto.releases)
        withContext(Dispatchers.IO) {
            releases.forEachIndexed { index, release ->
                launch {
                    val coverResult = searchEngine.searchCovers(release.id)
                    coverResult.forSuccess { coverDto ->
                        releases[index] = release.copy(coverImages = coverDto.images)
                        send(artistDto.copy(releases = releases).toArtistReleases())
                    }
                }
            }
        }
    }.buffer(Channel.CONFLATED)
        .distinctUntilChanged()
}