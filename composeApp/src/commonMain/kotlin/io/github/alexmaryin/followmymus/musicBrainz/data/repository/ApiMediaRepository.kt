package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.MediaDao
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.*
import io.github.alexmaryin.followmymus.musicBrainz.data.utils.httpsReplace
import io.github.alexmaryin.followmymus.musicBrainz.domain.CoversEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.MediaRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.WorkState
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Media
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single

@Single(binds = [MediaRepository::class])
class ApiMediaRepository(
    private val searchEngine: SearchEngine,
    private val coversEngine: CoversEngine,
    private val mediaDao: MediaDao
) : MediaRepository {
    override val workState = MutableStateFlow(WorkState.IDLE)

    override val mediaCount = MutableStateFlow<Int?>(null)

    private val _errors = Channel<ErrorType>()
    override val errors = _errors.receiveAsFlow()

    override fun getReleaseMedia(releaseId: String): Flow<List<Media>> = mediaDao.getReleaseMedia(releaseId)
        .map { it.map { media -> media.toMedia() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun fetchReleasesMedia(releaseId: String) {
        workState.update { WorkState.LOADING }
        mediaCount.update { null }

        val remoteResponse = searchEngine.searchMedia(releaseId)
        remoteResponse.forSuccess { response ->

            mediaCount.update { response.count }
            val mediaDto = response.releases

            mediaDao.insertMedia(
                media = mediaDto.map { it.toEntity(releaseId) },
                items = mediaDto.flatMap { media ->
                    media.items.map { item ->
                        item.toEntity(media.id)
                    }
                },
                tracks = mediaDto.flatMap { media ->
                    media.items.flatMap { item ->
                        item.tracks.map { track ->
                            track.toEntity(media.id, item.id)
                        }
                    }
                },
                resources = mediaDto.flatMap { media ->
                    media.resources.map { it.toMediaResourceEntity(media.id) }
                }
            )
            workState.update { WorkState.COVERING }
            mediaDto.asFlow().flatMapMerge(50) { media ->
                flow {
                    emit(coversEngine.getMediaCovers(media.id) to media)
                }
            }.collect { (result, media) ->
                result.forSuccess { coverDto ->
                    val previewUrl = coverDto.images.selectCover { selectPreview() }?.httpsReplace()
                    val largeUrl = coverDto.images.selectCover { url }?.httpsReplace()
                    mediaDao.updateMediaCovers(media.id, previewUrl, largeUrl)

                }
                result.forError { error ->
                    _errors.send(error.type)
                }
            }
        }
        remoteResponse.forError { error ->
            _errors.send(error.type)
        }
        workState.update { WorkState.IDLE }
    }
}