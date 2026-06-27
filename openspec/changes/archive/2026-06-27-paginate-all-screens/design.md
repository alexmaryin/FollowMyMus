## Context

FollowMyMus uses AndroidX Paging 3 in two places today. Search Artists drives a custom `HandlePagingItems` DSL (`core/ui/pagingHandler.kt`) that centralizes the four load-state branches (`OnEmpty / OnRefresh / OnError / OnSuccess`) and a few `LazyListScope` extensions (`onPagingItems / onAppendItem / onLastItem`). The repository at `musicBrainz/data/repository/ApiArtistsRepository.kt` returns `Flow<PagingData<Artist>>` from a custom `ArtistsPagingSource` and feeds a `searchCount: StateFlow<Int?>` side-channel for total count. Favorites also returns `Flow<PagingData<FavoriteArtist>>`, but from a Room `PagingSource` and a per-sort `flatMapLatest`, and the UI in `FavoritesPanelUi.kt` bypasses the wrapper to do its own `items()` + `itemKey` because grouped headers require `insertSeparators` (which the wrapper does not expose). Releases and Media are not paginated: the repositories expose `Flow<Map<String, List<Release>>>` and `Flow<List<Media>>` respectively, and the UI loads them in full.

The wrapper's surface area is the right shape (a DSL over `LazyPagingItems`) but the implementation is fragile:

- `PagingHandlerScope` holds `private val loadState = derivedStateOf { items.loadState }.value` — a snapshot, not a reactive read; it only "works" because the scope is re-created on every recomposition of `HandlePagingItems`.
- Branches short-circuit through a `handled: Boolean` flag. The flag is a state machine: `OnEmpty / OnRefresh / OnError` claim it in declaration order, `OnSuccess` claims it last. Reordering the DSL changes behavior. The only thing protecting the caller is convention.
- `OnError` maps a fixed set of Ktor exception types into `SearchError` (a `musicBrainz.domain.models` type). `core` therefore depends on `musicBrainz`. Any screen that wants paging must accept this layering violation.
- There is no `onAppendError`. Append failures are silent.
- A refresh that fails *after* content has been loaded causes the entire list to disappear (replaced by the error placeholder). Standard Paging 3 idiom keeps the data and surfaces the error via Snackbar/Toast.
- `onPagingItems` does not take `contentType`, which blocks staggered/grid reuse.
- The wrapper has dead branches in practice: `ArtistsPanelUi` wraps the entire `HandlePagingItems` call in `if (state.isLoading) VinylLoadingIndicator(...) else HandlePagingItems(...)`, so `OnRefresh` and `OnEmpty` are unreachable on the only screen that uses the wrapper.

Page sizes are inline magic numbers in repositories (`PagingConfig(20, 40, prefetch=20, maxSize=100)` for favorites, `PagingConfig(50, 50)` for search). Total count is fed through a side-channel callback (`emitNewCount`) into a `MutableStateFlow` on the repository. Grouping logic lives in `screens/.../favorites/ui/favoritesPanel/FavoritesGrouping.kt` and is hard-coded to favorites (`SortArtists.ABC / COUNTRY / TYPE / DATE / NONE`). Releases' grouping by year bucket is in-memory in the repository and won't scale.

The goal of this change is to make the wrapper library-grade (so all data screens can adopt it), migrate Favorites onto it, and add paging to Releases and Media.

## Goals / Non-Goals

**Goals**

- A single domain-agnostic paging library in `core` that owns the wrapper, the grouping extension, the page-size defaults, and the total-count helper.
- A predictable, recomposition-correct sealed UI state for the wrapper.
- A caller-provided error mapping so any feature can plug in its own error type.
- Append-level error visibility (footer with retry).
- Refresh errors that do not destroy already-loaded data.
- Grouped headers as a first-class operation on `PagingData<T>`, independent of any specific sort key.
- `PagingConfig` constants in one place.
- All current data screens (Search Artists, Favorites, Releases, Media) drive their lists through the same wrapper.

**Non-Goals**

- `RemoteMediator` for offline-first paging. Releases and Media keep their current single-shot network refresh; paging is a UI concern only for now. (Tracked as a follow-up.)
- Search-by-similarity / faceting on the artists endpoint.
- Rewriting the favorites Supabase sync flow. `searchCount: StateFlow<Int?>` is replaced, but the `favorite_artists` table contract and the realtime channel stay as-is.
- New per-screen tweaks beyond what the wrapper supports. (If a screen needs a custom loading UX, it wraps the wrapper call the way Search Artists does today, and the wrapper's `OnRefresh / OnEmpty` branches stay documented as "use these unless you have a reason not to".)
- Tests for every permutation. We add a smoke test for the new wrapper state model and rely on the existing JVM test suite for the rest.

## Decisions

### 1. Sealed `PagingUiState<T>` over the `handled` flag

```kotlin
sealed interface PagingUiState<out T : Any> {
    data object Loading : PagingUiState<Nothing>
    data object Empty : PagingUiState<Nothing>
    data class Error(val error: PagingError) : PagingUiState<Nothing>
    data class Content<T : Any>(val items: LazyPagingItems<T>) : PagingUiState<T>
}
```

Computed once per scope from `items.loadState.refresh` and `items.itemCount`. The wrapper's `On*` methods become thin switches on this state. The declaration order in the DSL no longer affects behavior — there is exactly one state at a time.

**Alternative considered:** keep the flag-based DSL, just fix the order with a sealed type internally and surface the same API. Rejected: the flag is a code smell that the caller is forced to learn. Sealing the state is the same effort and gives a better mental model.

### 2. Caller-provided `Throwable -> PagingError` mapper

The wrapper exposes:

```kotlin
fun <T : Any> HandlePagingItems(
    items: LazyPagingItems<T>,
    errorMapper: (Throwable) -> PagingError = ::defaultPagingError,
    onErrorAction: (PagingError) -> Unit = {},
    content: @Composable PagingHandlerScope<T>.() -> Unit
)
```

`PagingError` is a sealed type in `core/paging`:

```kotlin
sealed interface PagingError {
    data class Network(val message: String?) : PagingError
    data class Server(val code: Int, val message: String) : PagingError
    data object InvalidResponse : PagingError
    data class Unknown(val message: String?) : PagingError
}
```

`core` no longer imports `musicBrainz`. Each screen can map domain exceptions (`SearchEngine.HttpError`, `MusicBrainzException`, etc.) into `PagingError` inside its mapper. `SearchError` stays in `musicBrainz.domain.models` for non-paging use cases and is replaced by a mapping function in the artists UI layer (`SearchError.toPagingError()`).

**Alternative considered:** keep `SearchError` in the wrapper and rename it `PagingError` inside `core`. Rejected: forces every consumer to either use `SearchError` or wrap it. The mapper is one more line of code at each call site and decouples the library.

### 3. Refresh-error-preserves-content

```kotlin
val refreshState = loadState.refresh
when {
    refreshState is LoadState.Loading && items.itemCount == 0 -> PagingUiState.Loading
    refreshState is LoadState.Error && items.itemCount == 0 -> PagingUiState.Error(...)
    refreshState is LoadState.Error -> PagingUiState.Content(items).also {
        scope.onErrorAction(refreshState.error)  // caller shows snackbar
    }
    items.itemCount == 0 -> PagingUiState.Empty
    else -> PagingUiState.Content(items)
}
```

A refresh failure on an already-populated list keeps the list and runs the caller's `onErrorAction` (which the caller wires to a Snackbar host). The error placeholder only appears when there is nothing to show.

**Alternative considered:** always show the error placeholder. Rejected: hostile UX; the user loses 100 loaded items because the network blipped. Snackbar is the standard Paging 3 idiom.

### 4. Append error footer with retry

`onAppendError { error, retry -> ... }` is a new method on `PagingHandlerScope`. It runs when `loadState.append is LoadState.Error`. Inside the lambda the caller typically renders a row with an error message and a "Retry" button that calls `retry()`. The wrapper provides the retry closure; the caller decides the visual.

**Alternative considered:** swallow append errors and log. Rejected: silent paging failure is a Paging-3 anti-pattern; users will see a list that "just stops" with no recourse.

### 5. `PagingData<T>.groupedBy<K>` as a generic extension

```kotlin
fun <T : Any, K : Any> PagingData<T>.groupedBy(
    keySelector: (T) -> K
): PagingData<GroupedItem<T, K>>
```

Implemented with `insertSeparators`. `GroupedItem` is `sealed`:

```kotlin
sealed interface GroupedItem<out T, out K> {
    data class Header<K>(val key: K) : GroupedItem<Nothing, K>
    data class Item<T>(val value: T) : GroupedItem<T, Nothing>
}
```

Caller for Favorites:

```kotlin
pagingData.groupedBy<FavoriteArtist, SortKeyType> { artist ->
    when (sortType) {
        SortArtists.ABC -> SortKeyType.Abc(artist.sortName.first().uppercaseChar().toString())
        SortArtists.COUNTRY -> SortKeyType.Country(artist.country)
        // ...
    }
}
```

Caller for Releases:

```kotlin
pagingData.groupedBy<Release, Int> { it.firstReleaseDate?.year ?: 0 }
```

Lives in `core/paging/PagingDataGrouping.kt`. The favorites-specific `FavoritesGrouping.kt` becomes a thin file that just defines the `SortKeyType` enum and a `keySelector` factory. Sticky headers and the year-bucket header in releases both consume the same extension.

**Alternative considered:** keep grouping inside each screen. Rejected: the pattern repeats. A generic extension that takes a `keySelector` is the smallest reusable abstraction.

### 6. `PagingDefaults` as a single object

```kotlin
object PagingDefaults {
    const val API_PAGE = 50
    const val API_INITIAL = 50
    const val ROOM_PAGE = 20
    const val ROOM_INITIAL = 40
    const val PREFETCH = 20
    const val MAX_SIZE = 100

    fun apiConfig() = PagingConfig(pageSize = API_PAGE, initialLoadSize = API_INITIAL, enablePlaceholders = false)
    fun roomConfig() = PagingConfig(
        pageSize = ROOM_PAGE,
        initialLoadSize = ROOM_INITIAL,
        prefetchDistance = PREFETCH,
        enablePlaceholders = false,
        maxSize = MAX_SIZE
    )
}
```

Repositories import `PagingDefaults.apiConfig()` / `PagingDefaults.roomConfig()` instead of constructing `PagingConfig` literals. The constants are testable and the rationale is documented in one place.

**Alternative considered:** per-repository `PagingConfig` literals stay, but extracted to private `val`s. Rejected: still scattered; no shared tuning point.

### 7. `PagingData<T>.totalCount` Flow

Replaces the `emitNewCount` callback on `ApiArtistsRepository` and the `combine(favoriteDao.getTotalCount())` in the favorites flow. Two implementations:

- **Network source** (`PagingData<Artist>` from `ArtistsPagingSource`): the `PagingSource` already calls `emitNewCount(response.count)`; the extension wraps that as `Flow<Int?>` exposed on the repository. The PagingSource is registered with Koin via `@Factory(binds = [PagingSource::class])` and the count flow is collected by the caller via `repository.totalArtistCount` or similar.
- **Room source** (`PagingData<FavoriteArtist>` from `FavoriteDao`): combine with `favoriteDao.getTotalCount()` as today, but inside a single helper that returns `Flow<Int>`.

The `searchCount: StateFlow<Int?>` field on `ArtistsRepository` is removed. Each repository exposes its own count flow.

**Alternative considered:** keep `searchCount` as a global. Rejected: shared mutable state between two unrelated screens (search vs favorites) is a latent bug. Each repository owns its count.

### 8. Repository contract: `Flow<PagingData<T>>` everywhere

`ReleasesRepository.getArtistReleases(artistId: String): Flow<PagingData<Release>>`. The current `Flow<Map<String, List<Release>>>` is replaced — the year grouping moves to the UI via `groupedBy`. `MediaRepository.getReleaseMedia(releaseId: String): Flow<PagingData<Media>>` replaces `Flow<List<Media>>`.

**Breaking change.** Affected:
- `ReleasesRepository` interface and `ApiReleasesRepository` implementation
- `MediaRepository` interface and `ApiMediaRepository` implementation
- `ReleaseDao` gains a PagingSource query, loses the `Flow<List<ReleaseEntity>>` query (or keeps it for sync; the callers split)
- `MediaDao` same shape
- `ReleasesList` / `ReleasesPanelUI` / `ReleasesPanelTall` / `ReleasesPanelLanscape` switch to paged UI
- The media screen's data layer (under `screens/.../media/`) — exact path TBD, not yet read in this exploration
- `jvmTest` mocks for the changed repos

**Alternative considered:** introduce a parallel `getArtistReleasesPaged` and keep the old one. Rejected: doubles the API surface and the maintenance burden. The old `Flow<Map<...>>` is unreadable once paging is in; the only reason to keep it would be to defer migration, which is a fake reason.

### 9. PagingSource factory: lambda vs. Koin `@Factory(binds=[...])`

`ArtistsPagingSource` is currently `@Factory(binds = [PagingSource::class])` with `parametersOf(query, ::emitNewCount)`. That binds a single `PagingSource` type globally; if you add a `MediaPagingSource` with the same bound, Koin will resolve whichever was registered last.

**Decision:** change the pattern. The repository owns the `pagingSourceFactory` lambda and uses Koin's `get { parametersOf(...) }` *inside* the lambda:

```kotlin
override fun searchArtists(query: String): Flow<PagingData<Artist>> = Pager(
    config = PagingDefaults.apiConfig(),
    pagingSourceFactory = { get<ArtistsPagingSource> { parametersOf(query, ::emitNewCount) } }
).flow
```

`@Factory(binds = [PagingSource::class])` on `ArtistsPagingSource` is **dropped**; the class becomes a regular Koin-injectable factory. `MediaPagingSource` follows the same pattern. No global binding collisions.

**Alternative considered:** use Koin `@Named` qualifiers per source. Rejected: more verbose, same problem (still have to remember the qualifier at every call site). Lambda-in-repository is the KMP idiom.

## Risks / Trade-offs

- **Repository contract changes break the UI layer synchronously** → the Releases and Media UI changes must land in the same change as the repository changes. We will not stage them as separate PRs. *Mitigation:* the tasks.md file lists them as ordered, atomic steps.
- **`groupedBy` allocates one `GroupedItem` per page item** → the in-memory representation grows with `PagingData`'s loaded pages. For a screen with thousands of items, that's still O(visible pages), not O(all items). *Mitigation:* none needed; Paging 3 already keeps only the loaded pages resident.
- **`PagingData<T>.groupedBy` is a terminal transform** → once grouped, the caller cannot re-`map` the underlying `T` without re-doing the grouping. *Mitigation:* the caller applies `map` and any other transforms *before* `groupedBy`. This is documented in the extension's Kdoc.
- **The wrapper's `onErrorAction` callback fires during composition, not in a side-effect** → it must not allocate state or do work. *Mitigation:* the wrapper's contract says "fire-and-forget; the caller wires this to a Snackbar host via `LaunchedEffect(error)` inside the lambda". We document this clearly.
- **The new `LazyPagingItems<T>` access pattern means the wrapper depends on a newer `paging-compose` API** → `contentType` requires paging-compose 3.3+. *Mitigation:* the project is on 3.4.x per `build.gradle.kts`; verified safe.
- **`SearchError` removal requires every consumer to be updated** → only `ArtistsPanelUi` currently imports `SearchError` in the UI. *Mitigation:* the wrapper never had a stable contract (it's in `core/ui`, not a published module); rename is internal.
- **Tests will lag** → the `jvmTest` fakes will be updated for the new repository signatures, but we are not writing fresh tests for every paged screen. *Mitigation:* the new `paging` capability spec lists testable scenarios; each lands in the same PR as its consumer.
- **Migration ordering risk** → if the new wrapper is built but Favorites is not migrated, two paging patterns coexist in the codebase. *Mitigation:* the change is shipped as a single PR per the tasks list. No half-migrated state in `main`.

## Migration Plan

Single PR, ordered as listed in `tasks.md`:

1. Add `core/paging` package: `PagingDefaults`, `PagingDataExt` (totalCount), `PagingDataGrouping` (groupedBy). No consumers yet.
2. Rewrite `core/ui/pagingHandler.kt` with the sealed state, the error mapper, append error, refresh-saves-content. Keep the same `HandlePagingItems { ... }` DSL surface so consumers can move one at a time.
3. Migrate `FavoritesList` + `FavoritesPanelUi` to the new wrapper, drop the `FavoritesGrouping.groupedBy` in favor of the generic extension.
4. Migrate `ArtistsPanelUi` to the new wrapper. Remove `SearchError` import from `core/ui`. Add a `SearchError.toPagingError()` extension in the artists UI package.
5. Replace `searchCount: StateFlow<Int?>` with per-repository totalCount flows. Drop the `emitNewCount` callback path on `ArtistsPagingSource` once a direct count flow is wired.
6. Add `MediaPagingSource` and `MediaDao.getPagedReleaseMedia` query. Change `MediaRepository.getReleaseMedia` to `Flow<PagingData<Media>>`. Migrate the media screen.
7. Add `ReleasesDao.getPagedReleasesByDate` (or similar) Room PagingSource. Change `ReleasesRepository.getArtistReleases` to `Flow<PagingData<Release>>`. Update `ReleasesList` + `ReleasesPanelUI` to use the wrapper with `groupedBy { it.firstReleaseDate?.year ?: 0 }`.
8. Update `jvmTest` fakes for the changed repository signatures.
9. Run `./gradlew :composeApp:assembleDebug :composeApp:jvmTest`. Manual smoke on Android emulator.

Rollback: revert the single PR. No data migration, no schema change.

## Open Questions

- Should `core/paging` and `core/ui` remain separate packages, or should the whole paging layer live under one `core/paging` package with a thin `core/ui/pagingHandler.kt` re-export? The current proposal keeps them separate (lib vs. UI binding) but the split is not load-bearing. *Resolution needed before code lands.*
- Should `PagingError` live in `core/paging/PagingError.kt` or in a `core/error` package? If we add more cross-cutting error helpers later, `core/error` is more discoverable. *Trivial; default to `core/paging` for now and revisit if the error vocabulary grows.*
- For the `totalCount` extension on network sources, should the count be a `Flow<Int>` (non-null, first value is `0`) or `Flow<Int?>` (nullable, `null` until the first page returns)? The current `searchCount: StateFlow<Int?>` is nullable. *Resolution needed for test stability; default to `Flow<Int?>` for backward compatibility.*
- Should the Releases PagingSource group in Room (i.e., a `@Query` that returns `PagingSource` ordered by year bucket directly) or in the UI via `groupedBy`? The latter is more flexible (caller can change the bucket function without a Room schema change). *Default: UI-side grouping, like Favorites. Revisit if performance becomes a concern.*
- The `media` screen path has not been read in this exploration. The proposal lists it as a consumer of `MediaRepository.getReleaseMedia`, but the exact file paths and call sites are TBD. *Resolution: read `screens/.../media/` before starting the media migration tasks.*
