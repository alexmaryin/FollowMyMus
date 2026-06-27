package io.github.alexmaryin.followmymus.core.paging

import androidx.paging.PagingConfig
import io.github.alexmaryin.followmymus.core.paging.PagingDefaults.apiConfig
import io.github.alexmaryin.followmymus.core.paging.PagingDefaults.roomConfig

/**
 * Single source of truth for paging sizes.
 *
 * Repositories MUST use [apiConfig] / [roomConfig] rather than
 * constructing `PagingConfig` literals inline.
 */
object PagingDefaults {
    const val API_PAGE = 50
    const val API_INITIAL = 50

    const val ROOM_PAGE = 20
    const val ROOM_INITIAL = 40
    const val PREFETCH = 20
    const val MAX_SIZE = 100

    const val DEFAULT_MAX_RELEASE_PAGES = 200
    const val DEFAULT_MAX_MEDIA_PAGES = 200

    var MAX_RELEASE_PAGES = DEFAULT_MAX_RELEASE_PAGES
    var MAX_MEDIA_PAGES = DEFAULT_MAX_MEDIA_PAGES

    fun apiConfig() = PagingConfig(
        pageSize = API_PAGE,
        initialLoadSize = API_INITIAL,
        enablePlaceholders = false
    )

    fun roomConfig() = PagingConfig(
        pageSize = ROOM_PAGE,
        initialLoadSize = ROOM_INITIAL,
        prefetchDistance = PREFETCH,
        enablePlaceholders = false,
        maxSize = MAX_SIZE
    )
}
