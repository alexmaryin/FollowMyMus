package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.alexmaryin.followmymus.musicBrainz.data.model.mappers.toArtist
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.models.Artist
import org.koin.core.annotation.Factory

@Factory(binds = [PagingSource::class])
class ArtistsPagingSource(
    private val searchEngine: SearchEngine,
    private val query: String,
    private val emitNewCount: (Int?) -> Unit
) : PagingSource<Int, Artist>() {

    // Need to drop duplicates which could appear in API response with overlapping pages
    private val seenIds = ArrayDeque<String>()
    private fun addToSeenIds(id: String) {
        seenIds += id
        if (seenIds.size > DEQUE_LIMIT) seenIds.removeFirst()
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artist> {
        emitNewCount(null)
        val offset = params.key?.coerceAtLeast(0) ?: 0
        val limit = params.loadSize

        return try {
            val response = searchEngine.searchArtists(query, offset, limit)
            emitNewCount(response.count)
            val artists = response.artists.filter { it.id !in seenIds }.map { it.toArtist(false) }
            artists.forEach { artist -> addToSeenIds(artist.id) }
            LoadResult.Page(
                data = artists,
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

internal const val DEQUE_LIMIT = SearchEngine.LIMIT * 3 // sliding window for three pages