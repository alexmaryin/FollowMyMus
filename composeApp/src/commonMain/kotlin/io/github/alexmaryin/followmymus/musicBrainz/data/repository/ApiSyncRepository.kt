package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import io.github.alexmaryin.followmymus.core.ErrorType
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.SyncDao
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.toEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SyncStatus
import io.github.alexmaryin.followmymus.musicBrainz.domain.LocalDbRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.RemoteSyncStatus
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.SyncRepository
import io.github.alexmaryin.followmymus.supabase.domain.SupabaseDb
import io.github.alexmaryin.followmymus.supabase.domain.model.ArtistRemoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Factory

@Factory(binds = [SyncRepository::class])
class ApiSyncRepository(
    private val syncDao: SyncDao,
    private val supabaseDb: SupabaseDb,
    private val searchEngine: SearchEngine,
    private val dbRepository: LocalDbRepository
) : SyncRepository {

    private val _syncStatus = MutableStateFlow<RemoteSyncStatus>(RemoteSyncStatus.Idle)
    override val syncStatus: StateFlow<RemoteSyncStatus> = _syncStatus

    private val _hasPendingActions = MutableStateFlow(false)
    override val hasPendingActions: StateFlow<Boolean> = _hasPendingActions

    override suspend fun syncRemote() {
        val errors = mutableListOf<ErrorType>() // accumulate all errors

        _syncStatus.update { RemoteSyncStatus.Process }
        // remove first local ids marked to pending remove
        val pendingRemove = syncDao.getArtistsIdsPendingRemove()
        if (pendingRemove.isNotEmpty()) {
            val removed = supabaseDb.bulkRemoveFavoriteArtists(pendingRemove)
            removed.forSuccess {
                syncDao.bulkDeleteArtistsById(pendingRemove)
            }
            removed.forError { errors += it.type }
        }

        // sync artists with local-first approach
        val localIdsToPush = syncDao.getIdsToPushAsList().toSet()
        val localIds = syncDao.getIdsAsList().toSet()
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
                    syncDao.markAsSynced(toPush)
                }
                pushed.forError { errors += it.type }
            }

            toFetch.chunked(SearchEngine.LIMIT)
                .filter { it.isNotEmpty() }
                .forEach { ids ->
                    val response = searchEngine.fetchArtistsById(ids)
                    response.forSuccess { artists ->
                        dbRepository.bulkInsertArtists(artists) { toEntity(true, SyncStatus.OK) }
                    }
                    response.forError { errors += it.type }
                }

            if (toRemoveLocal.isNotEmpty()) {
                syncDao.bulkDeleteArtistsById(toRemoveLocal.toList())
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
        _hasPendingActions.update { syncDao.hasPendingActions() }
    }
}