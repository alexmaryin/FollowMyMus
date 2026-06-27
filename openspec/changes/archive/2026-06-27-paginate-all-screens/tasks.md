## 1. Core paging library

- [x] 1.1 Create `core/paging/PagingError.kt` with the sealed `PagingError` type (`Network`, `Server`, `InvalidResponse`, `Unknown`) and a `defaultPagingError(Throwable): PagingError` function.
- [x] 1.2 Create `core/paging/PagingDefaults.kt` with `API_PAGE = 50`, `API_INITIAL = 50`, `ROOM_PAGE = 20`, `ROOM_INITIAL = 40`, `PREFETCH = 20`, `MAX_SIZE = 100`, and the `apiConfig()` / `roomConfig()` factory functions.
- [x] 1.3 Create `core/paging/PagingDataExt.kt` exposing `Flow<PagingData<T>>.totalCount(): Flow<Int?>` as a no-op helper, with concrete helpers `RoomPagingCount<T>(dao, query)` and `NetworkPagingCount<T>()` that the repositories instantiate.
- [x] 1.4 Create `core/paging/PagingDataGrouping.kt` with the `GroupedItem<T, K>` sealed type and the `PagingData<T>.groupedBy<K>(keySelector: (T) -> K): PagingData<GroupedItem<T, K>>` extension. Implement with `insertSeparators`. Document that callers should `map` *before* `groupedBy`.

## 2. Paging wrapper rewrite

- [x] 2.1 Define `PagingUiState<T : Any>` sealed interface in `core/ui/pagingHandler.kt` with `Loading`, `Empty`, `Error(PagingError)`, `Content(LazyPagingItems<T>)` variants.
- [x] 2.2 Replace `PagingHandlerScope.loadState` (currently a snapshot) with `private val loadState: State<LoadStates> = remember { derivedStateOf { items.loadState } }` — read `loadState.value` inside each branch.
- [x] 2.3 Compute the `PagingUiState<T>` once per recomposition from `loadState.value` and `items.itemCount`. Branch methods (`OnLoading`, `OnEmpty`, `OnError`, `OnContent`) become thin `when` arms on the sealed state.
- [x] 2.4 Add `errorMapper: (Throwable) -> PagingError` and `onErrorAction: (PagingError) -> Unit` parameters to `HandlePagingItems`. Default `errorMapper = ::defaultPagingError`. Default `onErrorAction` is a no-op. Refresh-error-preserves-content: when `Content` is the chosen state AND the refresh is in error, invoke `onErrorAction` exactly once per error transition.
- [x] 2.5 Add `onAppendError { error, retry -> ... }` and `onAppendLoading { ... }` slots on the scope. `onAppendError` exposes a `retry: () -> Unit` closure that calls `items.retry()`. Both slots are gated on `loadState.value.append`.
- [x] 2.6 Add `contentType: (T) -> Any?` parameter to `onPagingItems(key = ..., contentType = ..., body = ...)`. Forward to `LazyListScope.items`.
- [x] 2.7 Drop the `musicBrainz.domain.models.SearchError` import from `core/ui/pagingHandler.kt`. Confirm the file compiles with no `musicBrainz` dependency.
- [x] 2.8 Remove the old `handled: Boolean` field and the `if (handled) return` guards. The single sealed state replaces them.

## 3. Search Artists migration

- [x] 3.1 Add `SearchError.toPagingError(): PagingError` extension in `musicBrainz/domain/models/` so the artists UI layer can map domain errors into the wrapper's generic type. Map `InvalidResponse -> PagingError.InvalidResponse`, `NetworkError(msg) -> PagingError.Network(msg)`, `ServerError(code, msg) -> PagingError.Server(code, msg)`.
- [x] 3.2 Migrate `ArtistsPanelUi.kt` to consume the new wrapper API. Pass `errorMapper = { (it as? Exception)?.toPagingError() ?: PagingError.Unknown(it.message) }`. Replace `OnSuccess` with `OnContent`. Remove the dead `OnRefresh` / `OnEmpty` calls (the screen keeps its own `VinylLoadingIndicator` for the first-load case).
- [x] 3.3 Replace `repository.searchCount: StateFlow<Int?>` with `repository.totalArtistCount: Flow<Int?>` driven by a `NetworkPagingCount` that the artists repository owns. The artists UI binds `state.searchResultsCount` to this new flow.
- [x] 3.4 Drop the `emitNewCount` callback parameter on `ArtistsPagingSource`. Replace with a one-shot `MutableStateFlow<Int?>` the source updates on each successful page load. The repository combines that flow into `totalArtistCount`.

## 4. Favorites migration

- [x] 4.1 Update `FavoritesList.kt` to use the new wrapper. Pass `errorMapper = { PagingError.Unknown(it.message) }` (favorites errors are local and rarely network; we map everything to `Unknown` until a more specific error type exists).
- [x] 4.2 Update `FavoritesPanelUi.kt` to consume `HandlePagingItems`. Replace the raw `items()` + `itemKey` block with `OnContent { onPagingItems(...) { when (item) { ... } } }`. Use `onAppendLoading` for the append spinner.
- [x] 4.3 Replace `FavoritesGrouping.groupedBy` with a thin call to `PagingData<FavoriteArtist>.groupedBy<FavoriteArtist, SortKeyType> { artist -> ... }` where the `keySelector` switches on `sortType`. Move the file to a `favorites/ui/favoritesPanel/SortKeySelector.kt` (single-responsibility: just the keySelector factory).
- [x] 4.4 Verify that the favorites total count (currently emitted via the `emitNewCount` callback in `ApiArtistsRepository.getFavoriteArtists`) is wired into the new `totalCount` helper. Drop the side-channel.

## 5. Releases pagination

- [x] 5.1 Read `screens/mainScreen/pages/sharedPanels/ui/releasesPanel/` and `domain/releasesPanel/ReleasesList.kt` end-to-end so the migration of the UI is unambiguous. Document any landscape vs tall differences in a comment.
- [x] 5.2 Add `getPagedArtistReleases(artistId: String): PagingSource<Int, ReleaseEntity>` to `ReleaseDao`, ordered by `firstReleaseDate DESC`. Keep `getArtistReleases(artistId: String): Flow<List<ReleaseEntity>>` for the sync-on-open path (or remove if the sync can be driven by the count + paging source alone).
- [x] 5.3 Change `ReleasesRepository.getArtistReleases(artistId: String): Flow<PagingData<Release>>` in the domain interface. Update `ApiReleasesRepository` to build a `Pager(PagingDefaults.roomConfig())` over the new DAO query, `.map` to `Release` domain, and expose `totalReleasesCount: Flow<Int?>` from a `SELECT COUNT(*)` query.
- [x] 5.4 Migrate `ReleasesList.kt` to expose `releases: Flow<PagingData<Release>>` instead of `Flow<Map<String, List<Release>>>`. Drop the `groupByCategories()` mapping in the repository.
- [x] 5.5 Migrate `ReleasesPanelUI.kt` to call `HandlePagingItems(releases.collectAsLazyPagingItems(), errorMapper = ::toPagingError) { OnContent { ... } }`. Add a year-bucket grouping via `pagingData.groupedBy<Release, Int> { it.firstReleaseDate?.year ?: 0 }` before the items reach the wrapper.
- [x] 5.6 Update `ReleasesPanelTall.kt` and `ReleasesPanelLanscape.kt` to render the grouped `PagingData<GroupedItem<Release, Int>>` instead of the `Map<String, List<Release>>`. Headers are `GroupedItem.Header(year)`; items are `GroupedItem.Item(release)`.

## 6. Media pagination

- [x] 6.1 Locate and read the media screen code (`screens/mainScreen/pages/.../media/`). Document the call sites of `MediaRepository.getReleaseMedia` so the migration is unambiguous.
- [x] 6.2 Add `getPagedReleaseMedia(releaseId: String): PagingSource<Int, MediaWithData>` to `MediaDao`, ordered by `position ASC` (or whatever the existing ordering is).
- [x] 6.3 Create `MediaPagingSource` in `musicBrainz/data/repository/` for the API path. Page size `PagingDefaults.API_PAGE = 50`. Reuse the same dedup-via-`seenIds` sliding window pattern from `ArtistsPagingSource` (or extract it to a base class — see task 6.8).
- [x] 6.4 Change `MediaRepository.getReleaseMedia(releaseId: String): Flow<PagingData<Media>>` in the domain interface. Update `ApiMediaRepository` to build a `Pager(PagingDefaults.roomConfig())` over the DAO query, with the API sync still happening on empty cache as a one-shot.
- [x] 6.5 Migrate the media screen UI to call `HandlePagingItems(...) { OnContent { ... } }`. Drop the `collectAsStateWithLifecycle` and switch to `collectAsLazyPagingItems`.
- [x] 6.6 Wire the empty-cache → `searchMedia(releaseId)` sync into the new component. The component triggers the sync the same way `ReleasesList` does: if the first paging load is empty, run the sync; once Room is populated, paging starts returning items automatically.
- [x] 6.7 Wire `totalMediaCount: Flow<Int?>` from a `SELECT COUNT(*)` query and surface it in the UI header.
- [ ] 6.8 Optional refactor: extract the dedup sliding window from `ArtistsPagingSource` into a small `DeduplicatingPagingSource<T>` base class if `MediaPagingSource` needs the same logic. **Skipped** — the MusicBrainz media endpoint does not return overlapping pages, so `MediaPagingSource` has no `seenIds` window.

## 7. Tests

- [x] 7.1 Update the `jvmTest` MockK fakes for `ArtistsRepository`, `ReleasesRepository`, `MediaRepository` to match the new method signatures (return `Flow<PagingData<...>>` for the paged methods, expose `totalCount` flows).
- [x] 7.2 Add a JVM test that drives `PagingHandlerScope` through each `PagingUiState` variant (Loading, Empty, Error, Content-after-refresh-fails) and asserts the right branch fires.
- [x] 7.3 Add a JVM test for `PagingData<T>.groupedBy` confirming header insertion at group boundaries and Item wrapping for the originals.
- [x] 7.4 Add a JVM test for the `PagingDefaults.apiConfig()` / `roomConfig()` factories returning the documented page sizes.

## 8. Build & verify

- [x] 8.1 Run `./gradlew :composeApp:assembleDebug`. Fix any compile errors. KSP will need to re-run if any Koin module definitions change.
- [x] 8.2 Run `./gradlew :composeApp:jvmTest`. All updated fakes must pass and the new paging-library tests must succeed.
- [x] 8.3 Run `./gradlew clean :composeApp:assembleDebug` to confirm a clean build succeeds (KSP / BuildKonfig sometimes cache stale generated code).
- [ ] 8.4 Manual smoke: launch the app on Android, exercise Search Artists, Favorites (with sorting changes), Releases (open an artist, scroll, pull-to-refresh), and Media (open a release, scroll, pull-to-refresh). Verify refresh-fails-on-populated-list keeps the data and surfaces a Snackbar. Verify append error footer renders and retries.
