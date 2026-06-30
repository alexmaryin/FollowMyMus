## Context

The New Releases page replaces `DummyPage` at `PagerConfig.Releases` (currently mapped in `MainPagerComponent.kt:60`). Users have favorite artists stored in Room (synced with Supabase). MusicBrainz exposes `GET /ws/2/release-group?query=...` returning release-groups filtered by `first-release-date` and `arid` (artist id), with a documented maximum of ~400 entities per query — sufficient for any realistic favorites list when batched.

The existing infrastructure provides:
- `SearchEngine` (already paginated via `offset`/`limit` per call) and `Ktor HttpClient` configured with `User-Agent: FollowMyMus/1.0.0` and a 15s/3-retry backoff (`AppModule.provideMusicBrainzClient()`).
- `HandlePagingItems` Compose wrapper for Loading/Empty/Error/Content states.
- `PagingData<T>.groupedBy<K>(keySelector)` for in-line section headers.
- `ReleaseListItem` (artist-decoupled card), `ArtistReleasesList` (generic over `GroupedItem<Release, String>`), `MediaPanelUi` (media grid + track dialog), and the `MediaDetails` ViewModel (takes `releaseId`).
- `FavoriteDao.getFavoriteArtistsIds(): Flow<List<String>>` and `ArtistEntity.isFavorite` for the source-of-truth list.
- A pre-existing bug: `ApiReleasesRepository.syncReleases` calls `searchEngine.searchReleases(artistId, offset, limit)` in a tight loop with no rate limit. MB limits anonymous clients to 1 req/s (a documented 100%-503 enforcement, not 429-style throttling) — this loop will lock the user out within seconds under load. The new design must fix this.
- A DataStore preferences pattern: `PreferenceSource` is a `@Single` class wrapping the shared `Prefs` (`DataStore<Preferences>`). It exposes `Flow<T>` getters and `suspend` setters, with keys defined inline as `stringPreferencesKey("…")`. The account screen consumes it via `rememberAppPreferences(rememberPrefs())` + `collectAsStateWithLifecycle(default)`. Root preferences are applied via a `LaunchedEffect(prefs)` in `PreferencesHandler`. This is the pattern app settings should follow.

The new-releases directory `screens/mainScreen/pages/newReleases/` already exists but only contains a throwaway `SearchPage` preview stub to be replaced.

## Goals / Non-Goals

**Goals:**
- Display a grouped, paginated list of release-groups from favorite artists released within a recent window.
- Three-state per-row lifecycle (UNSEEN → SEEN → DISMISSED) preserved across re-syncs via idempotent upsert.
- Honor MusicBrainz's 1 req/s source-IP rate limit for **all** MusicBrainz calls (not just new code).
- Auto-sync on page open; pull-to-refresh on demand; first sync is 30-day window, subsequent syncs are incremental.
- Store app settings (`newReleasesLastOpenedDay`, `newReleasesLastSyncCompletedAt`) in DataStore preferences, mirroring the existing `PreferenceSource` pattern. No Room entity, no DAO, no Room migration caused by app settings.
- Bump DB to v2 with destructive migration **for the new `new_releases` table only** (dev-only; no real data at risk). App settings are unaffected by the destructive migration because they live in a separate file.
- Reuse existing components: `ReleaseListItem`, `ArtistReleasesList`, `MediaPanelUi`, `HandlePagingItems`, `MediaDetails`. Extract the `MediaDetails` factory for sharing.

**Non-Goals:**
- Real-time push notifications. Sync is page-open triggered. (WorkManager / FCM is a future change.)
- Cross-device sync of "seen/dismissed" state. Per-device only. The settings live in DataStore (per-app, per-device).
- Persisting favorite-artist coverage gaps. If a non-favorite artist releases something, it does not appear.
- Showing the release-group's full tracklist on the list (only the card). Track list opens via the existing media-details panel.
- Sticky headers. In-line only, matching the existing per-artist releases panel. (Sticky headers have a known scroll-state bug with `LazyList` lifecycle in this codebase.)
- Parallelizing API calls. Sequential is required by the rate limit and is sufficient.
- Promoting `Prefs` to a Koin `@Single`. The `DataStore<Preferences>` instance continues to be created per Composable via `rememberPrefs()`. The `ApiNewReleasesRepository` receives a `PreferenceSource` (which itself holds `Prefs`); the host Composable that owns the repository is responsible for the `Prefs` lifetime via the existing `rememberPrefs()` pattern.

## Decisions

### Decision 1: Search endpoint with `arid` OR-chain, not per-artist browse

**Choice:** Use `GET /ws/2/release-group?query=firstreleasedate:[startDay TO today] AND arid:(id1 OR id2 OR ... OR idN)&limit=100&offset=M&fmt=json`. Batch ids in groups of 50. Note: the search-index field is `firstreleasedate` (no hyphens); the response field on the wire is `first-release-date` (kebab-case) — only the unhyphenated form is legal in the `query=` parameter.

**Rationale:** Per-artist browse (`/artist/{id}/release-groups`) would require N HTTP requests for N favorites — defeats the rate limit. The search endpoint's `arid` field accepts a documented OR-chain of up to ~400 entities. With 50 ids per batch we stay well under the cap, and one request returns the union for all 50 artists in the window.

**Alternatives considered:**
- *Per-artist browse, batched over favorites*: 1 req per artist at 1 req/s = 25s for 25 favorites. The 50-id search batch gives 1 req for 50 artists. Two orders of magnitude better.
- *Use the existing `/artist/{id}/` endpoint with `inc=release-groups`*: same N-request problem, plus it doesn't accept a date filter.
- *Use the MusicBrainz collections API*: requires the user to be a logged-in MB editor. Out of scope.

### Decision 2: 50-id batch, paginated at 100 results

**Choice:** 50 favorite-artist ids per request; `limit=100` results per page (MB's documented max for `release-group` search); loop `offset` until `offset >= count`.

**Rationale:** 50 ids keeps the query well under the 400-entity OR-chain limit and produces a query string of ~1 KB — well under MB's documented URL-length tolerance. `limit=100` minimizes the page-loop depth: an artist with 250 new release-groups over 30 days would only need 3 page requests within a single 50-id batch. Sequential per-page iteration is fine because the next request goes out 1s later, which is also the rate-limit interval.

**Alternatives considered:**
- *100 ids per request*: hits the 400-id OR-chain limit when combined with `release-group-count` and date bounds. Not enough headroom.
- *25 ids per request*: safer for the OR-chain but doubles the request count. 50 is the sweet spot.
- *limit=25 (MB default)*: 4× more page requests. Reject.

### Decision 3: 30-day initial window, then incremental from `newReleasesLastOpenedDay`

**Choice:** On first sync ever, fetch the last 30 days. On subsequent syncs, fetch from `newReleasesLastOpenedDay` (exclusive) to today. Persist `newReleasesLastOpenedDay` only on **successful completion** of the full sync.

**Rationale:** The 30-day window is a generous on-ramp for new users (a 30-day backlog is interesting but not overwhelming) and matches what the user typically cares about. Subsequent opens only need 1–3 days. Writing the floor on success means a failed sync loses no unseen releases — the next attempt re-fetches the same window. Writing on start would silently skip the in-flight day on failure.

**Alternatives considered:**
- *Always 30 days*: stable but wasteful; we re-fetch releases the user has already seen.
- *7-day window*: too narrow for first sync. Users with infrequent app opens would miss releases.
- *Write floor on start*: failure means a skipped day. Rejected.
- *Track `lastSeenTimestamp` per release*: per-row floor is more flexible but adds a column the rate-limited query already implies. Rejected — keep the schema minimal.

### Decision 4: In-line headers, not sticky

**Choice:** Group by `artistName` via the existing `PagingData<T>.groupedBy` extension. Render with `ArtistReleasesList`, which already produces in-line (non-sticky) headers. Same look as the per-artist catalog page.

**Rationale:** Matches the existing per-artist releases panel for visual consistency, and avoids the LazyList scroll-state lifecycle bug that sticky headers introduce in this codebase (the scroll position drops to the start on list re-composition). The cost is "header scrolls with content" — acceptable for a feed of 10–50 entries.

**Alternatives considered:**
- *Sticky headers via `Modifier.stickyHeader`*: known to reset scroll position in this codebase. Rejected.
- *No grouping (flat list)*: loses the "this release is by who" context — the card already shows artist? Not currently. Adding artist name to the card itself duplicates work the header does. Flat list rejected.

### Decision 5: Three-state model on a dedicated `new_releases` table

**Choice:** New Room table `new_releases` with a state column. State transitions: `UNSEEN` (default on insert) → `SEEN` (on `MediaDetails` open) → `DISMISSED` (on swipe-to-dismiss). No reversal.

**Rationale:** A separate table is decoupled from the per-artist catalog. Typical scale is 1–50 rows per favorite artist (the 30-day window), not the 100s of historical releases in `ReleaseEntity`. The state column needs to be preserved across re-syncs, which is solved by an `@Upsert` keyed on the release-group `id` with a conditional `state` write (only insert, never overwrite).

**Alternatives considered:**
- *Columns on `ReleaseEntity`*: mixes catalog and notification concerns. Rejected.
- *Two states (UNSEEN/SEEN) with a soft-delete*: works but requires a "where not deleted" clause everywhere. Three states with `DISMISSED` filters out cleanly.
- *Reverse from UNSEEN to SEEN*: user wants to "remind me about this again" — out of scope. Not in v1.

### Decision 6: Per-batch upsert preserves `state` on re-fetch

**Choice:** When re-syncing, the upsert must not overwrite `state` for an existing row. Implementation: a DAO method `upsertPreservingState(list)` that does `INSERT ... ON CONFLICT(id) DO NOTHING` for any row whose `id` already exists, and `INSERT` for new rows. To update mutable fields (title, disambiguation, cover URL) for an existing row, do a separate `UPDATE ... WHERE id IN (...)` only for the fields the API may have refreshed.

**Rationale:** The user has already opened a release and it's marked SEEN. A re-fetch must not reset it to UNSEEN — that would create a perpetual "new" indicator on something the user has already seen. The flow is: insert new (UNSEEN), update mutable fields on existing (preserve state). Two queries, both idempotent.

**Alternatives considered:**
- *Single upsert with `ON CONFLICT DO UPDATE SET state = excluded.state`*: would overwrite SEEN with UNSEEN on every re-fetch. Rejected.
- *Delete-then-insert*: clears the state column. Rejected.

### Decision 7: Rate-limited API queue (singleton)

**Choice:** New `RateLimitedApiQueue` (Koin `Single`) — a `CoroutineScope` (app-lifetime) containing a single-consumer coroutine that reads from a `Channel<suspend () -> Unit>` and applies `delay(1000)` between jobs. Public API: `suspend fun <T> enqueue(block: suspend () -> T): T` — submits the block to the channel and `await`s its `Deferred` result.

**Rationale:** MusicBrainz's 1 req/s limit is enforced on a source-IP basis with a 100% 503 on overflow, not a 429-with-Retry-After. The Ktor `HttpRequestRetry` plugin handles transient 503s but does not space requests out — it only retries on failure. Without a queue, a sync loop of 5 requests in 0.5s would lock the user out for the next 30s and beyond. The queue centralizes the spacing so every MusicBrainz call (new and existing) honors the limit.

**Alternatives considered:**
- *Per-call `delay(1000)` inline in repositories*: scatters rate-limit logic; easy to forget; doesn't help the pre-existing bug in `ApiReleasesRepository`.
- *Token bucket with refill = 1 token/sec*: equivalent in steady state but more complex than needed for "no overlap".
- *Reactive Flow with `sample` operator*: doesn't backpressure; same problem.
- *Reduce request count by avoiding the search endpoint*: per Decision 1, this is the cheapest source.

### Decision 8: App settings live in DataStore, not a Room singleton table

**Choice:** Store `newReleasesLastOpenedDay` and `newReleasesLastSyncCompletedAt` as two `stringPreferencesKey` entries in the existing `DataStore<Preferences>` (file `.preferences_pb`), exposed via new `getAppSettings(): Flow<AppSettings>` and `suspend fun setNewReleasesFloor(day: LocalDate, now: Instant)` extensions on `PreferenceSource`. The values are stored as ISO-8601 strings (`LocalDate.toString()` and `Instant.toString()`). There is NO Room entity, NO DAO, NO database migration for these keys.

**Rationale:** App settings are two scalar, single-valued pieces of state. The Room-entity alternative would be a singleton-row table (`id = 0`, two nullable columns) — the worst shape Room supports, with no upside over a typed key-value store. The project already has a `DataStore<Preferences>` instance in `Prefs`, already wrapped in a `PreferenceSource` `@Single` with a `Flow<T>` getter + `suspend` setter pattern. Adding two keys costs three lines per method and zero new infrastructure; adding a Room singleton table costs an entity class, a DAO, a KSP-codegen cycle, a `fallbackToDestructiveMigration` semantic to reason about, and ties settings lifetime to the `musicbrainz.db` file. Crucially, **the settings are not part of the user-facing data model** — they are implementation details of the sync algorithm — and so do not need to live alongside the data they describe. They survive the `new_releases`-driven v1→v2 destructive migration because `.preferences_pb` is a separate file from `musicbrainz.db`. The transactional guarantee on the floor-write is preserved: `prefs.edit { it[key1] = …; it[key2] = … }` runs the block atomically (DataStore serializes all writes through a single actor). The crash-safety profile matches the Room version: a crash between "all batches persisted" and "prefs.edit completes" leaves the floor at its old value, so the next sync re-fetches the same window — exactly the invariant we want.

**Alternatives considered:**
- *Singleton Room table with `id = 0`*: the originally-proposed design. Forces a destructive-migration semantic to ship two values that fit in a `DataStore<Preferences>` key. Rejected.
- *A new `AppSettingsSource` class (separate from `PreferenceSource`) as a `@Single`*: would work, but creates two Koin singletons for what is conceptually one preferences file. Rejected — keep all DataStore access in `PreferenceSource` (extensions live next to it in `preferences/AppSettings.kt`).
- *SharedPreferences (Android-only) or `NSUserDefaults` (iOS-only)*: not cross-platform; the project already standardized on DataStore. Rejected.
- *kotlinx.serialization of a JSON blob into a single `stringPreferencesKey("app_settings")`*: works but loses the per-key `Flow<T>` granularity the rest of `PreferenceSource` uses, and adds a serialization dependency. Rejected.

### Decision 9: Database v2 with destructive migration (for the new_releases table only)

**Choice:** Bump `MusicBrainzDatabase` to version 2. Add `fallbackToDestructiveMigration` to the database builder. The app is unpublished; the only user is the developer. The settings live in DataStore and are not part of this migration.

**Rationale:** The schema change is one new table (`new_releases`) and no column adds to existing tables. A real migration would need to be tested and would clutter the codebase. Destructive migration is correct for the dev phase. The DataStore-backed settings are unaffected because they are stored in `.preferences_pb`, a file independent of `musicbrainz.db`.

**Alternatives considered:**
- *Auto-migration with `AutoMigration(1, 2)`*: requires careful nullability and default-value handling; risks subtle data corruption. Not worth the engineering for dev-only.
- *Manual `Migration(1, 2)`*: overkill for dev-only.

### Decision 10: Shared `getMediaDetails` factory

**Choice:** Extract the `getMediaDetails(config: MediaDetailsConfig, context: ComponentContext): MediaDetails` factory into a top-level function in `screens/mainScreen/pages/sharedPanels/domain/mediaDetailsPanel/`. All three hosts (`FavoritesHost`, `ArtistsHost`, `NewReleasesHost`) call it from their `childPanels(extraFactory = ::getMediaDetails)`.

**Rationale:** Currently the factory is duplicated in each host (FavoritesHost lines 75–94, ArtistsHost lines 60–73). With a third host, duplication is no longer maintainable. A single shared factory gives one source of truth for the panel key, default back-stack behavior, and ViewModel binding.

**Alternatives considered:**
- *Inline copy-paste in `NewReleasesHost`*: three places to keep in sync. Rejected.
- *A `SharedPanels` object that owns all factories*: over-engineering for one factory.

### Decision 11: Cover URL from search response, fallback to media-details fetch

**Choice:** The search response's `ReleaseGroupDto` includes a `primary-type`, an `artist-credit` array (the source of truth for the entity's denormalized `artistId`/`artistName`), and a `first-release-date`. **It does NOT include** `secondary-types` (the field is only populated on the full release-group resource, not the search result), nor cover URLs. Cover URLs come from a separate call to `coverartarchive.org/release-group/{id}` (different rate limit). The card uses a placeholder until the user opens the release, at which point `MediaDetails` fetches the media list (and per-media cover URLs).

**Rationale:** The search endpoint doesn't carry cover metadata — that's by design (MB is data, Cover Art Archive is art). Adding a second call per release-group would double the network traffic and the rate-limit pressure. The card can render without a cover; once the user opens the release they get the proper media grid with covers.

**Alternatives considered:**
- *Per-row cover fetch in `NewReleasesRepository`*: doubles the request count. Rejected.
- *Coil `AsyncImage` with null URL fallback*: works, but adds a network call we don't need to make.

## Risks / Trade-offs

- **[Risk] MB rate limit enforced at 100% 503 (not 429) — one bad client can lock out the user's IP for a while.** → All MusicBrainz calls go through `RateLimitedApiQueue`. The queue's 1 req/s spacing makes it impossible to exceed the limit by accident. Ktor's retry (3 attempts, 3s/6s/9s) absorbs a 503 if MB's enforcement is asynchronous.
- **[Risk] Cancellation mid-batch leaves a partial state in Room.** → Per-batch upserts mean a half-synced state is recoverable: the next page-open re-fetches the same window. No row is ever partially written. `WorkState` reports `PARTIAL_SYNC` so the UI can show a snackbar.
- **[Risk] Crash between "all batches persisted" and "prefs.edit completes" leaves the floor at its old value.** → Identical to the Room version's crash profile. The next sync re-fetches the same window — a safe idempotent retry. No release is "skipped forever" because of a single crash.
- **[Risk] OR-chain of 50 ids in a single query string.** → Tested in MB docs: 400-entity limit applies to all queries, and 50 ids plus a date range plus other operators is well under 1 KB. We log the query size in debug builds for visibility.
- **[Risk] First-ever sync with empty settings and 1000 favorites takes ~25s.** → Acceptable; user sees a loading state. 50% of users have <50 favorites → 1 request → <1s. Pull-to-refresh is always available.
- **[Risk] `DISMISSED` rows accumulate forever.** → Acceptable for v1: a typical user dismisses a few per month, so the table grows slowly. A `cleanupDismissedOlderThan(threshold)` maintenance method is added but not auto-invoked; the user can manually clear or it can run on app start.
- **[Risk] `searchReleaseGroups` returns 1000s of results for very prolific artists over 30 days.** → Capped at 200 pages × 100 = 20 000 results (the same cap as the existing `MAX_RELEASE_PAGES`). The "200" cap matches `ApiReleasesRepository`'s pre-existing safety net.
- **[Risk] DataStore `edit` failure (disk full, permission denied) leaves the floor unwritten.** → Matches the existing `PreferenceSource` pattern: the write is wrapped in a try/catch that prints the error to the console and continues. The sync still completes (the user sees the new releases) — the next sync just re-fetches the same window. No data loss.
- **[Risk] Settings outlive the Room destructive migration but then appear "stale" relative to the freshly-emptied `new_releases` table.** → Acceptable and intended: after the v1→v2 migration, the user sees an empty list and a sync starts. The floor may still be at the old `newReleasesLastOpenedDay`, which causes a 1–3-day incremental sync. This is the safest behavior — never miss a release because of a migration.
- **[Risk] Sharing the `MediaDetails` factory breaks a host.** → `FavoritesHost` and `ArtistsHost` are unaffected; the factory extraction is a pure refactor. Tests for those hosts (if any) keep passing.
- **[Risk] `LocalDate`/`Instant` ISO-8601 strings drift if kotlinx-datetime changes its `toString()` format.** → The format is part of kotlinx-datetime's stable public API and is mandated by the ISO-8601 standard. A change would be a major version bump. Acceptable for v1.

## Migration Plan

- **No production migration.** The app is pre-release. The first install after this change ships with DB v2 and a fresh local cache. Existing v1 caches are dropped on open (destructive migration). DataStore preferences (`.preferences_pb`) are NOT dropped — they survive the destructive migration because they are a separate file.
- **Rollback**: revert the commit. No data loss because the v1 data was already only test data, and DataStore preferences are recreated on first sync.

## Open Questions

- **Should the search response be enriched to include `primary-type-id` for filtering? Out of scope for v1; releases are not filtered by type.**
- **Should the auto-sync be deferred until after the favorites sync completes? Today, `FavoritesHost` and `ArtistsHost` both call `syncRepository.syncStatus` — if a sync is already running, the new releases sync should probably wait or skip. Decision: skip if `syncRepository.syncStatus.value` is in a non-idle state, retry on the next page open.**
- **Should `newReleasesLastOpenedDay` be updated on `PARTIAL_SYNC` too, or only on full success? Decision: only on full success, per Decision 3. The rationale is "the user should not lose unseen releases" — partial sync means some were fetched, some weren't, so the safe move is to leave the floor where it is and retry next open.**
- **Should the DataStore-backed `AppSettings` be its own `@Single` class, or extension functions on `PreferenceSource`? Decision: extension functions in `preferences/AppSettings.kt` (top-level `fun PreferenceSource.getAppSettings(): Flow<AppSettings>` and `suspend fun PreferenceSource.setNewReleasesFloor(day: LocalDate, now: Instant)`). This keeps `PreferenceSource` focused on the existing theme/language/dynamic-mode getters, but allows `AppSettings` to be defined as a `data class` with proper `Flow<AppSettings>` typing. The companion-object `get(...)` cache in `PreferenceSource` deduplicates instances per process.**
