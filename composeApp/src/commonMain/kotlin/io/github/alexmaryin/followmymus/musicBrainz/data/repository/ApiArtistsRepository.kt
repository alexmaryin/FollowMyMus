package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.core.paging.NetworkPagingCount
import io.github.alexmaryin.followmymus.core.paging.PagingDefaults
import io.github.alexmaryin.followmymus.core.paging.RoomPagingCount
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ArtistDao
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.ArtistDto
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.FavoriteDao
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.toEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.toFavoriteArtist
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SyncStatus
import io.github.alexmaryin.followmymus.musicBrainz.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.LocalDbRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.SortArtists
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import io.github.alexmaryin.followmymus.supabase.data.mappers.toRemote
import io.github.alexmaryin.followmymus.supabase.domain.SupabaseDb
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

@Factory(binds = [ArtistsRepository::class])
class ApiArtistsRepository(
    private val artistDao: ArtistDao,
    private val favoriteDao: FavoriteDao,
    private val supabaseDb: SupabaseDb,
    private val searchEngine: SearchEngine,
    private val dbRepository: LocalDbRepository
) : ArtistsRepository, KoinComponent {

    private val networkCount = NetworkPagingCount()
    override val totalArtistCount: Flow<Int?> = networkCount.flow

    private val favoritesCount = RoomPagingCount { favoriteDao.getTotalCount() }
    override val totalFavoritesCount: Flow<Int?> = favoritesCount.flow

    override fun searchArtists(query: String): Flow<PagingData<Artist>> = Pager(
        config = PagingDefaults.apiConfig(),
        pagingSourceFactory = { get<ArtistsPagingSource> { parametersOf(query, networkCount) } }
    ).flow

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFavoriteArtists(sort: Flow<SortArtists>): Flow<PagingData<FavoriteArtist>> =
        sort.distinctUntilChanged().flatMapLatest { sortType ->
            val pagingSourceFactory = when (sortType) {
                SortArtists.NONE -> favoriteDao::getPagedArtists
                SortArtists.DATE -> favoriteDao::getPagedArtistsSortedByDate
                SortArtists.COUNTRY -> favoriteDao::getPagedArtistsSortedByCountry
                SortArtists.ABC -> favoriteDao::getPagedArtistsSortedByAbc
                SortArtists.TYPE -> favoriteDao::getPagedArtistsSortedByType
            }

            Pager(
                config = PagingDefaults.roomConfig(),
                pagingSourceFactory = pagingSourceFactory
            ).flow.map { pagingData ->
                pagingData.map { artistWithRelations -> artistWithRelations.toFavoriteArtist() }
            }
        }

    override fun getFavoriteArtistsIds(): Flow<List<String>> = favoriteDao.getFavoriteArtistsIds()

    override suspend fun cacheArtist(artistId: String) {
        if (artistDao.isArtistExists(artistId)) return
        val artist = searchEngine.getArtistFromCache(artistId) ?: return
        dbRepository.insertArtist(artist) {
            toEntity(false, SyncStatus.PendingRemoteRemove)
        }
    }

    override suspend fun addToFavorite(artistId: String) {
        val artist = searchEngine.getArtistFromCache(artistId)
            ?: searchEngine.getArtistById(artistId)?.dtoSource
            ?: return
        dbRepository.insertArtist(artist) { toEntity(true, SyncStatus.PendingRemoteAdd) }
        val remoteAdd = supabaseDb.addRemoteFavoriteArtist(artist.toRemote())
        remoteAdd.forSuccess {
            artistDao.updateSyncStatus(artist.id, SyncStatus.OK)
        }
    }

    override suspend fun addToFavoritesBulk(artists: List<ArtistDto>) {
        if (artists.isEmpty()) return
        dbRepository.bulkInsertArtists(artists) { toEntity(true, SyncStatus.PendingRemoteAdd) }
        val remoteResult = supabaseDb.bulkAddFavoriteArtists(artists.map { it.toRemote() })
        remoteResult.forSuccess {
            artists.forEach { artistDao.updateSyncStatus(it.id, SyncStatus.OK) }
        }
    }

    override suspend fun deleteFromFavorites(artistId: String) {
        artistDao.updateSyncStatus(artistId, SyncStatus.PendingRemoteRemove, false)
        val remoteRemove = supabaseDb.removeRemoteFavoriteArtist(artistId)
        remoteRemove.forSuccess {
            artistDao.deleteArtist(artistId)
        }
    }
}
