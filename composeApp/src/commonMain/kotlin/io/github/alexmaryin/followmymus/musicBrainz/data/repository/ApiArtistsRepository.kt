package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.model.api.enums.SyncStatus
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.MusicBrainzDAO
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toFavoriteArtist
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.RemoteSyncStatus
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

    private val _syncStatus = MutableStateFlow<RemoteSyncStatus>(RemoteSyncStatus.Idle)
    override val syncStatus: StateFlow<RemoteSyncStatus> = _syncStatus

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
        // TODO remove after debug
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
        // TODO remove after debug
        remoteRemove.forError { error ->
            println(error.type)
            println(error.message)
        }
    }

    override suspend fun syncRemote() {
        val errors = mutableListOf<ErrorType>() // collect all errors in process

        _syncStatus.update { RemoteSyncStatus.Process }
        // remove first local ids marked to pending remove
        val pendingRemove = musicBrainzDAO.getArtistsIdsPendingRemove()
        if (pendingRemove.isNotEmpty()) {
            val removed = supabaseDb.bulkRemoveFavoriteArtists(pendingRemove)
            removed.forSuccess {
                musicBrainzDAO.bulkDeleteArtistsById(pendingRemove)
            }
            removed.forError { errors += it.type }
        }

        // sync artists with local-first approach
        val localIdsToPush = musicBrainzDAO.getIdsToPushAsList().toSet()
        val localIds = musicBrainzDAO.getIdsAsList().toSet()
        val remote = supabaseDb.getRemoteFavoritesArtists()
        remote.forSuccess { remoteArtists ->
            val remoteIds = remoteArtists.map(ArtistRemoteEntity::artistId).toSet()
            // Set of ids to push from local which are not on remote.
            val toPush = localIdsToPush - remoteIds
            // Set of ids from remote which are not present locally - need to be fetched.
            val toFetch = remoteIds - localIds
            // Artists present locally but not on remote, excluding those just added locally.
            // These were likely removed from another device.
            val toRemoveLocal = (localIds - remoteIds) - localIdsToPush

            if (toPush.isNotEmpty()) {
                val remoteEntities = toPush.map { ArtistRemoteEntity(it) }
                val pushed = supabaseDb.bulkAddFavoriteArtists(remoteEntities)
                pushed.forSuccess {
                    musicBrainzDAO.markFavoriteArtistsAsSynced(toPush)
                }
                pushed.forError { errors += it.type }
            }

            toFetch.chunked(SearchEngine.LIMIT)
                .filter { it.isNotEmpty() }
                .forEach { ids ->
                val response = searchEngine.fetchArtistsById(ids)
                response.forSuccess { artists ->
                    if (artists.isNotEmpty()) {
                        musicBrainzDAO.bulkInsertArtistsDto(artists)
                    }
                }
                response.forError { errors += it.type }
            }

            if (toRemoveLocal.isNotEmpty()) {
                musicBrainzDAO.bulkDeleteArtistsById(toRemoveLocal.toList())
            }

            checkPendingActions()

            _syncStatus.update { RemoteSyncStatus.Idle }
        }
        remote.forError { errors += it.type }

        // emit error status if any error occurred
        if (errors.isNotEmpty()) {
            _syncStatus.update { RemoteSyncStatus.Error(errors) }
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