package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.ArtistDao
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.FavoriteDao
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.toEntity
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.toFavoriteArtist
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.enums.SyncStatus
import io.github.alexmaryin.followmymus.musicBrainz.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.LocalDbRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.favoritesPanel.SortKeyType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.SortArtists
import io.github.alexmaryin.followmymus.screens.utils.sortAbcOrder
import io.github.alexmaryin.followmymus.screens.utils.sortCountryOrder
import io.github.alexmaryin.followmymus.screens.utils.sortDateCategoryGroups
import io.github.alexmaryin.followmymus.screens.utils.toDateCategory
import io.github.alexmaryin.followmymus.supabase.data.mappers.toRemote
import io.github.alexmaryin.followmymus.supabase.domain.SupabaseDb
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalCoroutinesApi::class)
@Factory(binds = [ArtistsRepository::class])
class ApiArtistsRepository(
    private val artistDao: ArtistDao,
    private val favoriteDao: FavoriteDao,
    private val supabaseDb: SupabaseDb,
    private val searchEngine: SearchEngine,
    private val dbRepository: LocalDbRepository
) : ArtistsRepository, KoinComponent {

    override val searchCount = MutableStateFlow<Int?>(null)
    private fun emitNewCount(count: Int?) {
        searchCount.update { count }
    }

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

    override fun getFavoriteArtists(sort: Flow<SortArtists>) =
        sort.distinctUntilChanged().flatMapLatest { sortType ->
            val sortedFavorites = when (sortType) {
                SortArtists.NONE -> favoriteDao.getArtists()
                SortArtists.DATE -> favoriteDao.getArtistsSortedByDate()
                SortArtists.COUNTRY -> favoriteDao.getArtistsSortedByCountry()
                SortArtists.ABC -> favoriteDao.getArtistsSortedByAbc()
                SortArtists.TYPE -> favoriteDao.getArtistsSortedByType()
            }.map { flow ->
                flow.map { it.toFavoriteArtist() }
            }
            val groupedFavorites = sortedFavorites.map { favorites ->
                when (sortType) {
                    SortArtists.NONE -> mapOf(SortKeyType.None to favorites)

                    SortArtists.DATE -> favorites.groupBy { it.createdAt.toDateCategory() }
                        .sortDateCategoryGroups()

                    SortArtists.COUNTRY -> favorites.groupBy { artist -> SortKeyType.Country(artist.country) }
                        .sortCountryOrder()

                    SortArtists.ABC -> favorites.groupBy { artist -> artist.sortName.first().titlecaseChar() }
                        .sortAbcOrder()

                    SortArtists.TYPE -> favorites.groupBy { artist -> SortKeyType.Type(artist.type) }
                }
            }
            groupedFavorites
        }

    override fun getFavoriteArtistsIds(): Flow<List<String>> = favoriteDao.getFavoriteArtistIds()

    override suspend fun cacheArtist(artistId: String) {
        if (artistDao.isArtistExists(artistId)) return
        val artist = searchEngine.getArtistFromCache(artistId) ?: return
        dbRepository.insertArtist(artist) {
            toEntity(false, SyncStatus.PendingRemoteRemove)
        }
    }

    override suspend fun addToFavorite(artistId: String) {
        val artist = searchEngine.getArtistFromCache(artistId) ?: return
        dbRepository.insertArtist(artist) { toEntity(true, SyncStatus.PendingRemoteAdd) }
        val remoteAdd = supabaseDb.addRemoteFavoriteArtist(artist.toRemote())
        remoteAdd.forSuccess {
            artistDao.updateSyncStatus(artist.id, SyncStatus.OK)
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