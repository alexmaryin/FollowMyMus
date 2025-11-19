package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.MusicBrainzDAO
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toArtist
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toEntity
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import io.github.alexmaryin.followmymus.supabase.data.mappers.toRemote
import io.github.alexmaryin.followmymus.supabase.domain.SupabaseDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

@Factory(binds = [ArtistsRepository::class])
class ApiArtistsRepository(
    private val musicBrainzDAO: MusicBrainzDAO,
    private val supabaseDb: SupabaseDb
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

    override fun getFavoriteArtists(): Flow<List<Artist>> = musicBrainzDAO.getFavoriteArtists().map {
        it.map { artistWithRelations ->
            artistWithRelations.toArtist()
        }
    }

    override fun getFavoriteArtistsIds(): Flow<List<String>> = musicBrainzDAO.getFavoriteArtistsIds()

    override suspend fun addToFavorite(artist: Artist) {
        artist.dtoSource?.let {
            musicBrainzDAO.insertFavoriteArtist(
                artist = it.toEntity(true),
                area = it.area?.toEntity(),
                beginArea = it.beginArea?.toEntity(),
                tags = it.tags.map { tag -> tag.toEntity(artist.id) }
            )
        }
        supabaseDb.addRemoteFavoriteArtist(artist.toRemote())
    }

    override suspend fun deleteFromFavorites(artistId: String) {
        musicBrainzDAO.deleteArtistById(artistId)
        supabaseDb.removeRemoteFavoriteArtist(artistId)
    }
}