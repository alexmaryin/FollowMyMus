package io.github.alexmaryin.followmymus.musicBrainz.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.core.requireError
import io.github.alexmaryin.followmymus.core.requireSuccess
import io.github.alexmaryin.followmymus.musicBrainz.data.mappers.toMedia
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.Media
import org.koin.core.annotation.Factory

/**
 * Network-backed [PagingSource] for release media.
 *
 * Currently the production flow drives pagination from Room
 * (`PagingDefaults.roomConfig()`) and runs a one-shot API sync on
 * empty cache, so this class is reserved for a future iteration
 * (e.g. RemoteMediator) and is not wired in the current UI.
 *
 * Page size is [SearchEngine.LIMIT] (50). Unlike the artists
 * source, media pages do not overlap in practice, so no
 * `seenIds` dedup sliding window is used here.
 */
@Factory
class MediaPagingSource(
    private val searchEngine: SearchEngine,
    private val releaseId: String,
) : PagingSource<Int, Media>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Media> {
        val offset = params.key?.coerceAtLeast(0) ?: 0
        val limit = params.loadSize

        val response = searchEngine.searchMedia(releaseId, offset, limit)
        return when (response) {
            is Result.Success -> {
                val media = response.value.releases.map { it.toMedia() }
                LoadResult.Page(
                    data = media,
                    prevKey = if (offset == 0) null else offset - limit,
                    nextKey = if (media.isNotEmpty()) offset + media.size else null
                )
            }
            is Result.Error -> LoadResult.Error(
                IllegalStateException("media fetch failed: ${response.type::class.simpleName} ${response.message ?: ""}")
            )
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Media>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(state.config.pageSize)
                ?: page?.nextKey?.minus(state.config.pageSize)
        }
}
