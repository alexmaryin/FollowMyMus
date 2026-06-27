## Why

Paging in FollowMyMus is inconsistent and the wrapper around it is fragile. Today only **Search Artists** uses the custom `HandlePagingItems` DSL; **Favorites** uses raw `items()` + `itemKey` because the wrapper does not support `insertSeparators` (grouped headers). **Releases** and **Media** are not paginated at all even though the MusicBrainz API already supports offset/limit and Room already has native `PagingSource` support. The wrapper itself has a fragile `handled: Boolean` flag (state machine disguised as a guard), a `SearchError` coupling that violates the `core` layer's layering, and no support for append-level errors or refresh-fails-after-content. Before we can paginate the remaining screens we have to fix the wrapper, then we can drive every data screen through it.

## What Changes

- **Rewrite the paging wrapper** at `core/ui/pagingHandler.kt`:
  - Replace the `handled: Boolean` flag with a single computed `sealed PagingUiState<T>` (`Loading`, `Empty`, `Error`, `Content`) the caller dispatches on.
  - Hold `loadState` as a `State<LoadStates>` (not a snapshot) so branches react to changes.
  - Accept a caller-provided `Throwable -> PagingError` mapper. `core` no longer depends on `musicBrainz`.
  - Add `onAppendError { error, retry }` so append-level failures are not silently swallowed.
  - Treat `refresh.error` as **additive** when content is already present (caller's snackbar host shows it; the list keeps existing items).
  - Drop dead branches that the Search Artists screen bypasses with its own loading indicator, **or** commit to using them — pick one path per screen and document it.
  - Add `contentType: (T) -> Any?` to `onPagingItems` for staggered/grid support.

- **Generalize grouping** in a sibling file:
  - Promote `FavoritesGrouping.groupedBy` to `PagingData<T>.groupedBy(keySelector: (T) -> K): PagingData<GroupedItem<T, K>>` in `core/ui/pagingGrouping.kt`.
  - Sort-by-headers (ABC, country, type, date) becomes a `keySelector` choice for the favorites caller.

- **Add `PagingDefaults`** in a new `core/paging/PagingDefaults.kt`:
  - `API_PAGE = 50`, `ROOM_PAGE = 20`, `ROOM_INITIAL = 40`, `PREFETCH = 20`, `MAX_SIZE = 100`.
  - Repositories import these instead of literal `PagingConfig(20, 40, ...)`.

- **Add `PagingData<T>.totalCount` Flow** helper:
  - For network sources: project to the API's `count` field via the `PagingSource`.
  - For Room sources: combine with a `COUNT(*)` Flow in the repository.
  - Replaces the ad-hoc `emitNewCount` callback on `ApiArtistsRepository.searchCount: StateFlow<Int?>`.

- **Migrate Favorites** off the raw `items()` path onto the new wrapper + `groupedBy`. Behavior unchanged from the user's perspective.

- **Paginate Releases** (`ReleasesRepository.getArtistReleases` returns `Flow<PagingData<Release>>` instead of `Flow<Map<String, List<Release>>>`). Room `PagingSource` ordered by `firstReleaseDate DESC`. Grouped headers still come from the UI via `groupedBy` (key = year bucket). **BREAKING** repository contract change.

- **Paginate Media** (`MediaRepository.getReleaseMedia` returns `Flow<PagingData<Media>>` instead of `Flow<List<Media>>`). New `MediaPagingSource` for the API path (the API is already offset/limit-paged). Offline-first is a follow-up: this change does not introduce `RemoteMediator` for media, only the local-then-network view; the network call is still done up-front as a one-shot refresh for now.

- **Drop `SearchError` from `core/ui`**. The wrapper is generic. Each screen maps its own errors.

## Capabilities

### New Capabilities

- `paging`: The reusable paging library — wrapper DSL, `PagingDefaults`, `PagingData<T>.totalCount` Flow, `PagingData<T>.groupedBy` extension. Domain-agnostic, lives in `core`. Spec covers the API surface, sealed UI state, error model, grouping contract.

- `releases`: The artist releases screen. Spec covers pagination, grouped headers by year, sync-on-open behavior, refresh-error handling.

- `media`: The release media screen. Spec covers pagination of tracks/tracks-of-media for a release, load states, and the network-refresh-on-open contract.

### Modified Capabilities

None. The existing screens (search, favorites) are refactors only — their user-visible behavior does not change enough to warrant a spec delta. (Favorites gains the same load-state UX the search screen has today, which is a UI consistency improvement, not a spec change.)

## Impact

- **Code**:
  - Rewrite: `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/core/ui/pagingHandler.kt`
  - New: `core/ui/pagingGrouping.kt`, `core/paging/PagingDefaults.kt`, `core/paging/PagingDataExt.kt`
  - Update: `musicBrainz/data/repository/ApiArtistsRepository.kt` (favorites flow uses new defaults + totalCount helper), `musicBrainz/data/repository/ArtistsPagingSource.kt` (uses new totalCount path), `musicBrainz/domain/ArtistsRepository.kt` (return types stay — they were already `Flow<PagingData<...>>`).
  - Migrate: `screens/mainScreen/pages/favorites/.../FavoritesPanelUi.kt` and `FavoritesList.kt` onto the new wrapper.
  - Migrate: `screens/mainScreen/pages/artists/.../ArtistsPanelUi.kt` to the new wrapper API (no behavior change, just consume the new shape).
  - **BREAKING** repo contract: `musicBrainz/domain/ReleasesRepository.kt`, `musicBrainz/data/repository/ApiReleasesRepository.kt`, `musicBrainz/data/local/dao/ReleaseDao.kt`. Returns change from `Flow<Map<String, List<Release>>>` / `Flow<List<Release>>` to `Flow<PagingData<Release>>` and gain a `PagingSource` query.
  - **BREAKING** repo contract: `musicBrainz/domain/MediaRepository.kt`, `musicBrainz/data/repository/ApiMediaRepository.kt`, `musicBrainz/data/local/dao/MediaDao.kt`. Returns change from `Flow<List<Media>>` to `Flow<PagingData<Media>>`. New `MediaPagingSource` for the API path.
  - **BREAKING** UI: `screens/mainScreen/pages/sharedPanels/ui/releasesPanel/ReleasesPanelUI.kt` + `ReleasesPanelTall.kt` + `ReleasesPanelLanscape.kt` switch to `collectAsLazyPagingItems` + `HandlePagingItems`.
  - **BREAKING** UI: the media screen (currently consumes `Flow<List<Media>>` somewhere under `screens/mainScreen/pages/.../media/`) switches to paged.

- **Tests**: any test fakes or mocks of the changed repository interfaces need updating. The current `jvmTest` suite has MockK-based fakes — they will need new method signatures or new `Flow<PagingData<...>>` returns.

- **Dependencies**: no new libraries. `androidx.paging:paging-compose` 3.4.x (already in `build.gradle.kts`) covers the new `contentType` parameter and `LazyPagingItems.loadState` API.

- **Supabase**: not affected. All paging changes are local (Room) or MusicBrainz-API. Supabase realtime sync of favorites still flows through `favorite_artists` table — no schema change.
