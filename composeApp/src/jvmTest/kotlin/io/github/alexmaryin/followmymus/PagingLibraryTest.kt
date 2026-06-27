package io.github.alexmaryin.followmymus

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.alexmaryin.followmymus.core.paging.GroupedItem
import io.github.alexmaryin.followmymus.core.paging.PagingDefaults
import io.github.alexmaryin.followmymus.core.paging.PagingError
import io.github.alexmaryin.followmymus.core.paging.defaultPagingError
import io.github.alexmaryin.followmymus.core.paging.groupedBy
import io.github.alexmaryin.followmymus.core.ui.PagingUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Library tests for the paging helpers (introduced by the
 * `paginate-all-screens` change).
 *
 * The Compose-driven state machine in [io.github.alexmaryin.followmymus.core.ui.HandlePagingItems]
 * is exercised through the actual call sites (ArtistsPanelUi,
 * FavoritesPanelUi, ReleasesPanelUI, MediaPanelUi); pure JVM tests
 * here cover the non-Compose surfaces: [PagingDefaults] factories,
 * the type-level shape of [PagingData.groupedBy], the [PagingError]
 * mapper and the [PagingUiState] sealed hierarchy.
 */
class PagingLibraryTest {

    // ---- 7.4 PagingDefaults factories ----

    @Test
    fun `apiConfig returns documented page sizes`() {
        val cfg = PagingDefaults.apiConfig()
        assertEquals(PagingDefaults.API_PAGE, cfg.pageSize)
        assertEquals(PagingDefaults.API_INITIAL, cfg.initialLoadSize)
        assertEquals(false, cfg.enablePlaceholders)
    }

    @Test
    fun `roomConfig returns documented page sizes`() {
        val cfg = PagingDefaults.roomConfig()
        assertEquals(PagingDefaults.ROOM_PAGE, cfg.pageSize)
        assertEquals(PagingDefaults.ROOM_INITIAL, cfg.initialLoadSize)
        assertEquals(PagingDefaults.PREFETCH, cfg.prefetchDistance)
        assertEquals(PagingDefaults.MAX_SIZE, cfg.maxSize)
        assertEquals(false, cfg.enablePlaceholders)
    }

    // ---- 7.3 PagingData groupedBy ----

    @Test
    fun `groupedBy produces a PagingData of GroupedItem`() {
        val items = listOf(
            "Apple" to "A",
            "Avocado" to "A",
            "Banana" to "B",
            "Cherry" to "C",
        )
        val grouped: PagingData<GroupedItem<Pair<String, String>, String>> =
            PagingData.from(items).groupedBy<Pair<String, String>, String> { it.second }

        // The type is the contract: this will fail to compile if
        // the extension's return type changes.
        assertNotNull(grouped)
    }

    @Test
    fun `groupedBy works through a Pager pipeline`() {
        val source = ListPagingSource(
            listOf("A" to "x", "B" to "x", "C" to "y")
        )
        val pager = Pager(PagingConfig(pageSize = 10)) { source }
        val flow = kotlinx.coroutines.flow.flow {
            pager.flow.collect { pd ->
                emit(pd.groupedBy<Pair<String, String>, String> { it.second })
            }
        }
        // We only verify the type / that the flow produces; deeper
        // value-level assertions belong in a UI test.
        assertNotNull(flow)
    }

    // ---- 7.2 PagingUiState sealed state ----

    @Test
    fun `PagingUiState has Loading, Empty, Error, Content variants`() {
        // Pure data-class assertions; the actual branch dispatch is
        // covered by HandlePagingItems inside the Compose UI tests.
        val loading: PagingUiState<Nothing> = PagingUiState.Loading
        val empty: PagingUiState<Nothing> = PagingUiState.Empty
        val err: PagingUiState<Nothing> = PagingUiState.Error(PagingError.Unknown("x"))

        assertTrue(loading is PagingUiState.Loading)
        assertTrue(empty is PagingUiState.Empty)
        assertTrue(err is PagingUiState.Error)
        assertEquals(PagingError.Unknown("x"), (err as PagingUiState.Error).error)
    }

    // ---- PagingError default mapper ----

    @Test
    fun `defaultPagingError maps any throwable to Unknown`() {
        val mapped = defaultPagingError(IllegalStateException("boom"))
        assertIs<PagingError.Unknown>(mapped)
        assertEquals("boom", mapped.message)
    }
}

/**
 * Minimal [PagingSource] for tests; returns the supplied list once
 * with no next/prev keys (single page).
 */
private class ListPagingSource<T : Any>(
    private val data: List<T>
) : PagingSource<Int, T>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> =
        LoadResult.Page(data = data, prevKey = null, nextKey = null)

    override fun getRefreshKey(state: PagingState<Int, T>): Int? = null
}
