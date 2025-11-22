package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.SyncStatus
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.MusicBrainzDAO
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toFavoriteArtist
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import io.github.alexmaryin.followmymus.supabase.data.mappers.toRemote
import io.github.alexmaryin.followmymus.supabase.domain.SupabaseDb
import io.github.alexmaryin.followmymus.supabase.domain.model.ArtistRemoteEntity
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

@Factory(binds = [ArtistsRepository::class])
class ApiArtistsRepository(
    private val musicBrainzDAO: MusicBrainzDAO,
    private val supabaseDb: SupabaseDb,
    private val searchEngine: SearchEngine
) : ArtistsRepository, KoinComponent {

    override val searchCount = MutableStateFlow<Int?>(null)
    private fun emitNewCount(count: Int?) {
        searchCount.update { count }
    }

    private val _syncStatus = MutableStateFlow(ArtistsRepository.RemoteSyncStatus.IDLE)
    override val syncStatus: StateFlow<ArtistsRepository.RemoteSyncStatus> = _syncStatus

    private val _hasPendingActions = MutableStateFlow(false)
    override val hasPendingActions: StateFlow<Boolean> = _hasPendingActions

    override fun searchArtists(query: String): Flow<PagingData<Artist>> {
        val pager = Pager(
            config = PagingConfig(
                pageSize = SearchEngine.LIMIT,
                initialLoadSize = SearchEngine.LIMIT,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                get<PagingSource<Int, Artist>> { parametersOf(query, ::emitNewCount) }
            }
        )
        return pager.flow
    }

    override fun getFavoriteArtists(): Flow<List<FavoriteArtist>> = musicBrainzDAO.getFavoriteArtists().map {
        it.map { artistWithRelations ->
            artistWithRelations.toFavoriteArtist()
        }
    }

    override fun getFavoriteArtistsIds(): Flow<List<String>> = musicBrainzDAO.getFavoriteArtistsIds()

    override suspend fun addToFavorite(artist: Artist) {
        artist.dtoSource?.let {
            musicBrainzDAO.insertFavoriteArtist(
                artist = it.toEntity(true, SyncStatus.PendingRemoteAdd),
                area = it.area?.toEntity(),
                beginArea = it.beginArea?.toEntity(),
                tags = it.tags.map { tag -> tag.toEntity(artist.id) }
            )
        }
        val remoteAdd = supabaseDb.addRemoteFavoriteArtist(artist.toRemote())
        remoteAdd.forSuccess {
            musicBrainzDAO.updateArtistSyncStatus(artist.id, SyncStatus.OK)
        }
        remoteAdd.forError { error ->
            println(error.type)
            println(error.message)
        }
    }

    override suspend fun deleteFromFavorites(artistId: String) {
        musicBrainzDAO.updateArtistSyncStatus(artistId, SyncStatus.PendingRemoteRemove, false)
        val remoteRemove = supabaseDb.removeRemoteFavoriteArtist(artistId)
        remoteRemove.forSuccess {
            musicBrainzDAO.deleteArtistById(artistId)
        }
        remoteRemove.forError { error ->
            println(error.type)
            println(error.message)
        }
    }

    override suspend fun syncRemote() {
        _syncStatus.update { ArtistsRepository.RemoteSyncStatus.PROCESS }
        // remove first local ids marked to pending remove
        val pendingRemove = musicBrainzDAO.getArtistsIdsPendingRemove()
        if (pendingRemove.isNotEmpty()) {
            // TODO delete
            println("Pending delete ids: $pendingRemove")
            supabaseDb.bulkRemoveFavoriteArtists(pendingRemove)
            musicBrainzDAO.bulkDeleteArtistsById(pendingRemove)
        }

        // sync artists with local-first approach
        val localIdsToPush = musicBrainzDAO.getIdsToPushAsList().toSet()
        val localIds = musicBrainzDAO.getIdsAsList().toSet()
        val remote = supabaseDb.getRemoteFavoritesArtists()
        remote.forSuccess { remoteArtists ->
            val remoteIds = remoteArtists.map(ArtistRemoteEntity::artistId).toSet()
            val toPush = localIdsToPush - remoteIds
            val toFetch = remoteIds - localIds
            val toRemoveLocal = localIds subtract remoteIds

            if (toPush.isNotEmpty()) {
                // TODO delete
                println("Pending push ids: $toPush")
                val remoteEntities = toPush.map { ArtistRemoteEntity(it) }
                supabaseDb.bulkAddFavoriteArtists(remoteEntities)
                musicBrainzDAO.markFavoriteArtistsAsSynced(toPush)
            }

            if (toFetch.isNotEmpty()) {
                // TODO delete
                println("Pending fetch ids: $toFetch")
                toFetch.chunked(SearchEngine.LIMIT).forEach { ids ->
                    val response = searchEngine.fetchArtistsById(ids)
                    if (response.artists.isNotEmpty()) {
                        musicBrainzDAO.bulkInsertArtistsDto(response.artists)
                    }
                }
            }

            if (toRemoveLocal.isNotEmpty()) {
                // TODO delete
                println("Pending remove local ids: $toRemoveLocal")
                musicBrainzDAO.bulkDeleteArtistsById(toRemoveLocal.toList())
            }

            checkPendingActions()

            _syncStatus.update { ArtistsRepository.RemoteSyncStatus.IDLE }
        }

        // emit error status if any error occurred
        remote.forError { error ->
            println(error.type)
            println(error.message)
            _syncStatus.update { ArtistsRepository.RemoteSyncStatus.ERROR }
        }
    }

    override suspend fun checkPendingActions() {
        _hasPendingActions.update { musicBrainzDAO.hasPendingActions() }
    }

    override suspend fun clearLocalData() {
        musicBrainzDAO.clearTags()
        musicBrainzDAO.clearArtists()
    }
}