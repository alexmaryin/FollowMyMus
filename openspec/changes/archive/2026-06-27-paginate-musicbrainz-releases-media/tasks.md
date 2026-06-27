## 1. Data model

- [x] 1.1 Add `releaseCount: Int = 0` field to `ArtistDto` mapped from `@SerialName("release-group-count")`. Default of `0` keeps the `/artist/` search response (which lacks the field) deserializable.
- [x] 1.2 Add a `MAX_RELEASE_PAGES` and `MAX_MEDIA_PAGES` constant (value `200`) to a new `core/paging/PagingDefaults.kt` neighbour or to `SearchEngine` companion. Cap = 200 pages × `API_PAGE` (50) = 10 000 items.

## 2. Releases repository

- [x] 2.1 Add `totalNetworkReleases: Flow<Int?>` to `ReleasesRepository` interface, backed by a `MutableStateFlow<Int?>` in `ApiReleasesRepository`. Update it with the latest `artistDto.releaseCount` seen across pages; default `null` when no sync has run.
- [x] 2.2 Rewrite `ApiReleasesRepository.syncReleases` to paginate: loop `searchReleases(artistId, offset, limit = API_PAGE)` from `offset = 0` until `offset >= releaseCount` or response page is empty or page count exceeds `MAX_RELEASE_PAGES`. Accumulate `releases` and `resources` lists.
- [x] 2.3 Persist each page in its own `transactionalDao.insertDetails(...)` call (one per page) so cancellation at page N keeps pages 0..N-1 in Room.
- [x] 2.4 Move the cover-art `flatMapMerge(concurrency = 25)` collector inside the per-page loop so cover fetching starts as soon as a page is persisted, not after the entire sync.
- [x] 2.5 On any per-page `Result.Error`, stop the loop, send the error to `_errors`, and emit a `WorkState.PARTIAL_SYNC` state (add to the `WorkState` enum) so the UI can show a "partial sync" snackbar.
- [x] 2.6 Respect cancellation: every page-loop iteration must check `coroutineContext.isActive` (or `currentCoroutineContext().ensureActive()`) before issuing the next request and before the per-page transaction.

## 3. Media repository

- [x] 3.1 Add `totalNetworkMedia: Flow<Int?>` to `MediaRepository` interface, backed by a `MutableStateFlow<Int?>` in `ApiMediaRepository`. Update it with the latest `response.count` seen across pages; default `null` when no sync has run.
- [x] 3.2 Rewrite `ApiMediaRepository.fetchReleasesMedia` to paginate: loop `searchMedia(releaseId, offset, limit = API_PAGE)` from `offset = 0` until `offset >= response.count` or response page is empty or page count exceeds `MAX_MEDIA_PAGES`. Accumulate `media`, `items`, `tracks`, `resources` lists across pages.
- [x] 3.3 Persist each page in its own `mediaDao.insertMedia(...)` call (one per page) so cancellation at page N keeps pages 0..N-1 in Room.
- [x] 3.4 Move the cover-art `flatMapMerge(concurrency = 50)` collector inside the per-page loop.
- [x] 3.5 On any per-page `Result.Error`, stop the loop, send the error to `_errors`, and emit a `WorkState.PARTIAL_SYNC` state.
- [x] 3.6 Respect cancellation: check `coroutineContext.isActive` before each request and each per-page transaction.

## 4. WorkState additions

- [x] 4.1 Add `PARTIAL_SYNC` to `WorkState` enum in `musicBrainz/domain/models/WorkState.kt`. This is the state the repository emits when a sync stops early due to the cap or a mid-loop error.
- [x] 4.2 Update any existing `WorkState` consumers (UI snackbar hosts, progress indicators) to handle `PARTIAL_SYNC` distinctly from `IDLE` — show a "partial sync" message until the next successful full sync.

## 5. Tests

- [x] 5.1 Add a `FakePagingSearchEngine` (or extend the existing one) that returns a sequence of paginated `ArtistDto` / `SearchMediaResponse` from a queue. Each `searchReleases` / `searchMedia` call dequeues the next response.
- [x] 5.2 Test `ApiReleasesRepository.syncReleases` with an artist that has 137 release-groups: assert the fake is called 3 times with offsets 0, 50, 100; assert all 137 releases are in Room; assert `totalNetworkReleases` emits 137.
- [x] 5.3 Test `ApiReleasesRepository.syncReleases` cancellation: launch in a `Job`, cancel after the first page is persisted, assert the second request is not made and the first page is in Room.
- [x] 5.4 Test `ApiReleasesRepository.syncReleases` cap: set `MAX_RELEASE_PAGES = 2` (test-only override), assert the loop stops at page 2, `WorkState.PARTIAL_SYNC` is emitted, and a "partial sync" signal is in `_errors`.
- [x] 5.5 Test `ApiMediaRepository.fetchReleasesMedia` with a release that has 124 tracks: assert the fake is called 3 times with offsets 0, 50, 100; assert all 124 tracks are in Room; assert `totalNetworkMedia` emits 124.
- [x] 5.6 Test `ApiMediaRepository.fetchReleasesMedia` cancellation and cap, mirroring 5.3 and 5.4.

## 6. Build & verify

- [x] 6.1 Run `./gradlew :composeApp:assembleDebug` and confirm it builds (no schema change, no KSP regeneration required unless a `@Module`/`@Single` annotation was added — none should be).
- [x] 6.2 Run `./gradlew :composeApp:jvmTest` and confirm the new tests pass.
- [ ] 6.3 Manual smoke test: open a real artist with > 50 release-groups (e.g. Radiohead) in `:androidApp` debug build, confirm all release-groups are visible after sync completes.
- [ ] 6.4 Manual smoke test: open a release with > 50 tracks (e.g. a long live album) in `:androidApp` debug build, confirm all tracks are visible after sync completes.
