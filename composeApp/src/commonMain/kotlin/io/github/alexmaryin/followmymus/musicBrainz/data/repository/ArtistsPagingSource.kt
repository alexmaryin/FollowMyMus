package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.alexmaryin.followmymus.musicBrainz.data.model.localDb.MusicBrainzDAO
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toArtist
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import org.koin.core.annotation.Factory

@Factory(binds = [PagingSource::class])
class ArtistsPagingSource(
    private val searchEngine: SearchEngine,
    private val musicBrainzDAO: MusicBrainzDAO,
    private val query: String,
    private val emitNewCount: (Int?) -> Unit
) : PagingSource<Int, Artist>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artist> {
        emitNewCount(null)
        val offset = params.key?.coerceAtLeast(0) ?: 0
        val limit = params.loadSize

        return try {
            val favoriteIds = musicBrainzDAO.getFavoriteArtistsIds()
            val response = searchEngine.searchArtists(query, offset, limit)
            emitNewCount(response.count)
            LoadResult.Page(
                data = response.artists
                    .distinctBy { it.id }  // Need to drop duplicates which could appear in API response
                    .map { it.toArtist(it.id in favoriteIds) },
                prevKey = if (offset == 0) null else offset - limit,
                nextKey = if (response.artists.isNotEmpty()) offset + response.artists.size else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Artist>): Int? {
        return state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(state.config.pageSize)
                ?: page?.nextKey?.minus(state.config.pageSize)
        }
    }
}