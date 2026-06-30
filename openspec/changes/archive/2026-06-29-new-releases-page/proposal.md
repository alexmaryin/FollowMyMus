## Why

The `PagerConfig.Releases` slot in `MainPagerComponent` is mapped to `DummyPage` — the "New Releases" page has been a placeholder since the pager was scaffolded. Users have favorited artists whose new music they want to follow, but the current app only surfaces an artist's full historical catalog (via `FavoritesList` → per-artist `ReleasesList`). There is no view of "what just came out from the artists I follow".

MusicBrainz exposes a search endpoint that returns release-groups filtered by `first-release-date` and `arid` (artist id), and supports batched queries of up to ~400 ids per request. We can poll this once per page-open, batched in groups of 50, and stream results into Room as each batch lands — giving the user a progressively filling feed of "new" releases with the MB-mandated 1 req/s rate limit honored.

The app settings (the sync floor and the "updated X minutes ago" timestamp) are tiny scalar values used only by the sync orchestrator. They are not a list, not relational, not queryable. Putting them in a Room table would be over-modeling: a single-row "singleton" table with nullable columns is the worst shape Room can be asked to model. The project already has a DataStore preferences pattern (theme, language, dynamic color mode) backed by `PreferenceSource` — a `DataStore<Preferences>` wrapper that the account screen consumes via `Flow<T>` and writes through `suspend fun`. New app settings should follow the same pattern for consistency and to avoid the v1→v2 destructive-migration churn.

## What Changes

- **Add a New Releases page** to the pager. Replace `PagerConfig.Releases -> DummyPage` with a real `NewReleasesHost` that displays a grouped, paginated list of release-groups from favorite artists released within a recent window.

- **Add `new_releases` Room table** with a three-state model: `UNSEEN` (dot + bold), `SEEN` (no dot, regular), `DISMISSED` (removed). Each row carries the denormalized artist name and cover URL for list rendering without joins.

- **Store app settings in DataStore preferences** (not a Room table) — `newReleasesLastOpenedDay: LocalDate?` (the sync floor) and `newReleasesLastSyncCompletedAt: Instant?` (for "updated X minutes ago" UI). The settings are added as two keys on the existing `PreferenceSource` (`DataStore<Preferences>`) using `stringPreferencesKey("…")` with ISO-8601 `LocalDate` / `Instant` string values, mirroring the `getThemeMode` / `changeThemeMode` pattern. Settings are written only on successful sync completion; never on start or on partial failure. No new DAO, no new entity, no Room migration triggered by these keys.

- **Add `NewReleasesRepository.syncNewReleases()`** that batched-searches MusicBrainz. Each batch of 50 favorite-artist ids is sent as a single `firstreleasedate:[startDay TO today] AND arid:(id1 OR ... OR idN)` query (note: `firstreleasedate` is the search-index field with no hyphens; the response field is `first-release-date` in kebab-case), paged at 100 results. Per-batch upserts preserve existing `state` so a user who has already seen or dismissed a release is never re-notified. The response's `artist-credit` array is the source of truth for the denormalized `artistId`/`artistName` on each `NewReleaseEntity` row — the repository picks the first credit whose id is in the user's favorites set, so a collaboration between a favorited and a non-favorited artist is attributed to the favorited one. At the end of a fully-successful sync the repository calls `appSettings.setNewReleasesFloor(today, now)` — a single atomic `prefs.edit { }` that updates both keys in one transaction.

- **Add a shared `RateLimitedApiQueue`** (singleton, app-lifetime) that serializes MB API calls with a 1-second delay between them. This is required by MB's documented 1 req/s source-IP limit (any over-rate returns 503 on **all** subsequent requests, not a partial decline). The queue plugs in at the Ktor request boundary and is cancelable via the caller's scope.

- **Retrofit `ApiReleasesRepository.syncReleases` to use the same queue.** This fixes a pre-existing bug: the per-artist sync loop currently hammers MB with no rate limit, which would 503-block the user's IP under load.

- **Add a new `SearchEngine.searchReleaseGroups(query, offset, limit)` method** that hits the search endpoint and returns `SearchReleaseGroupResponse` with `releaseGroups: List<ReleaseGroupDto>` and `count: Int`.

- **Reuse existing components** to avoid new UI surface area:
  - `ReleaseListItem` for each card (decoupled, no per-artist binding).
  - `ArtistReleasesList` for the list, with `groupedBy { it.artistName }` instead of the per-artist catalog's `groupedBy { it.primaryType }`. Headers stay in-line (not sticky) — same as the existing releases panel, and avoids the LazyList scroll-state bug that sticky headers introduce.
  - `MediaPanelUi` for the media grid and the track-list dialog. Shared wholesale.
  - `HandlePagingItems` for the Loading/Empty/Error/Content contract.

- **Share `MediaDetails` across hosts** by extracting the `getMediaDetails` factory into `sharedPanels` so `FavoritesHost`, `ArtistsHost`, and the new `NewReleasesHost` all use the same component instance and same back-stack entry.

- **Wire auto-sync on page open** via `lifecycle.doOnStart`, mirroring `FavoritesHost` and `ArtistsHost`. Manual pull-to-refresh via the existing `HandlePagingItems.onErrorAction` slot. First sync ever fetches the last 30 days; subsequent syncs fetch from `newReleasesLastOpenedDay` forward (typically 1–3 days, depending on how often the user opens the page).

- **Bump the database to version 2** with `fallbackToDestructiveMigration` enabled. The bump is caused by the new `new_releases` table only — the settings live in DataStore, so they neither force the version bump nor get caught by the destructive migration. The app is unpublished; the only user is the developer. No real data is lost.

## Capabilities

### New Capabilities

- `new-releases`: the New Releases page, its data model (`NewReleaseEntity` + three-state lifecycle), the batched-sync algorithm, the rate-limited MB queue, and the auto-sync trigger. Covers the user-visible behavior: opening the page starts a sync, results stream in, unseen items have a dot, opened items lose the dot, swiped items are removed. The DataStore-backed `AppSettings` source is a shared capability that this change contributes to (the existing `preferences` capability is informally extended).

### Modified Capabilities

None. The existing `releases` and `media` capabilities are unchanged at the requirement level — `ApiReleasesRepository.syncReleases` is refactored to use the new rate-limited queue, but its observable behavior (loads all release-groups, partial sync on cap, cancels gracefully) is preserved. The existing `releases` spec already covers those requirements.

## Impact

- **New files** (under `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/`):
  - `screens/mainScreen/pages/newReleases/` — Host, List ViewModel, UI, action types
  - `core/network/RateLimitedApiQueue.kt` — shared infra
  - `musicBrainz/data/repository/NewReleasesRepository.kt`
  - `musicBrainz/data/remote/model/ReleaseGroupDto.kt` and `SearchReleaseGroupResponse.kt`
  - `musicBrainz/data/local/dao/NewReleasesDao.kt`
  - `musicBrainz/data/local/model/NewReleaseEntity.kt`
  - `musicBrainz/data/local/model/NewReleaseState.kt`
  - `preferences/AppSettings.kt` — `data class AppSettings(…)` + `PreferenceSource.getAppSettings(): Flow<AppSettings>` and `suspend fun PreferenceSource.setNewReleasesFloor(day: LocalDate, now: Instant)` extensions

- **New schema**: bump `MusicBrainzDatabase` to version 2. One new table: `new_releases`. `fallbackToDestructiveMigration` handles the dev-time drop. KSP regenerates the database implementation. The settings are stored in DataStore and do NOT participate in the Room schema.

- **New Koin bindings** (in `appModule.kt` or a new `releasesModule.kt`):
  - `RateLimitedApiQueue` (singleton, app-lifetime)
  - `NewReleasesRepository` (single)
  - `NewReleasesDao` (single, from Room)
  - No binding needed for `AppSettings` — it is an extension on the existing `PreferenceSource` `@Single`, which is already in the graph.

- **Modified files**:
  - `screens/mainScreen/domain/mainScreenPager/MainPagerComponent.kt` — `PagerConfig.Releases -> DummyPage` becomes `PagerConfig.Releases -> NewReleasesHost`
  - `screens/mainScreen/pages/sharedPanels/domain/mediaDetailsPanel/MediaDetailsHost.kt` (or equivalent) — extract `getMediaDetails` factory for sharing
  - `screens/mainScreen/pages/favorites/domain/pageHost/FavoritesHost.kt` — use shared `getMediaDetails`
  - `screens/mainScreen/pages/artists/domain/pageHost/ArtistsHost.kt` — use shared `getMediaDetails`
  - `preferences/PreferenceSource.kt` — no change to existing methods; the new `getAppSettings` / `setNewReleasesFloor` are top-level extension functions in the new `AppSettings.kt` file (preferred over adding unrelated methods to the `PreferenceSource` class body — they belong to a different domain concept).
  - `musicBrainz/domain/SearchEngine.kt` — add `searchReleaseGroups(query, offset, limit)`
  - `musicBrainz/data/remote/ApiSearchEngine.kt` — implement the new method
  - `musicBrainz/data/repository/ApiReleasesRepository.kt` — wrap requests in `RateLimitedApiQueue.enqueue { ... }` (no observable behavior change, just rate-limited)
  - `musicBrainz/data/repository/ApiNewReleasesRepository.kt` (new) — inject `PreferenceSource` (in addition to `SearchEngine`, `RateLimitedApiQueue`, `NewReleasesDao`, `FavoriteDao`, `Clock`); read floor from `preferenceSource.getAppSettings().first()`; write floor with `preferenceSource.setNewReleasesFloor(today, now)` on successful completion.

- **Network**: ~1 HTTP request per 50 favorite artists on first sync (30-day window), and ~1–2 requests on subsequent opens (1–3 day window). At 1 req/s the worst case is 25s for 1000 favorites; the typical case (≤100 favorites) is 1–3 requests and 1–4 seconds. The Ktor `HttpRequestRetry` plugin (3 attempts, 3s/6s/9s backoff) handles MB 503s during 1-req/s traffic.

- **DB**: one new table (`new_releases`). Existing `release`, `media`, `favorite_artists`, `releases`, `artists` tables untouched. Version bump from 1 → 2 with destructive migration (dev only — no real data at risk). App settings are NOT in Room; they live in DataStore and survive the destructive migration (they live in `.preferences_pb`, not `musicbrainz.db`).

- **Tests** (under `composeApp/src/jvmTest/`):
  - `NewReleasesRepositoryTest` — batched sync with a `FakeSearchEngine`, preservation of `state` on re-upsert, batch-size boundaries, cancellation, partial-batch pagination (when 50 ids yield >100 results). Uses a `FakePreferenceSource` (or an in-test `MutableStateFlow<AppSettings?>`) to verify floor-write invariants.
  - `RateLimitedApiQueueTest` — verifies 1-second spacing between calls, cancel mid-queue, exception propagation.
  - Existing `ApiReleasesRepositoryTest` (if any) — should still pass after the queue retrofit.

- **UI**: no new layout primitives. Existing `LazyColumn`-based `ArtistReleasesList` is the only visual change point: switch its `groupedBy` keySelector from type to artist name.
