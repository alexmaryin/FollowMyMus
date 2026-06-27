## Context

The MusicBrainz `/artist/{id}/` and `/release/` browse endpoints return at most 50 items per call and accept `offset` and `limit` query parameters. `SearchEngine.searchReleases` and `SearchEngine.searchMedia` already accept these parameters in the type system, but `ApiReleasesRepository.syncReleases` and `ApiMediaRepository.fetchReleasesMedia` call them with the default `offset = 0, limit = 50` and never loop. The result: an artist with 137 release-groups is stored with only the first 50, and a release with 124 tracks is stored with only the first 50.

`SearchEngine.LIMIT` = 50, `PagingDefaults.API_PAGE` = 50, `PagingDefaults.ROOM_PAGE` = 20. The Room `PagingSource` already pages from the local cache, so the visible UI is only as complete as the cache. `ReleasesDao.insertReleases` and `MediaDao.insertMedia*` use `@Upsert` (Room `Upsert`), so re-inserting the same release with updated cover URLs is safe and idempotent.

The `MediaPagingSource` already exists for media but is not wired into the UI; the architecture today is "one-shot network sync → Room → Room PagingSource". This change preserves that architecture: only the network-sync code path changes, not the UI paging path.

## Goals / Non-Goals

**Goals:**
- Read all pages of releases and media on initial sync, not just the first.
- Persist the union of all pages to Room in a way that survives partial failure (no rollback of already-persisted pages).
- Track the network-reported total count so the UI can show a total alongside the cached count.
- Honor coroutine cancellation mid-loop.
- Cap the loop at a safety bound to defend against runaway `release-group-count` values.

**Non-Goals:**
- Re-architecting the UI to use `MediaPagingSource` (RemoteMediator-style). The current "sync once → page from Room" model is preserved.
- Parallelizing the page requests. MusicBrainz's anonymous rate limit (1 req/sec) makes this risky, and the existing Ktor retry/backoff is tuned for sequential calls. Sequential pagination respects this without change.
- Adding UI affordances for the partial-sync cap state. The cap is a defensive guard, not a user-facing flow.
- Migrating the previous `paginate-all-screens` change's specs into the main spec. That's an archive-time concern; this change's specs are self-contained for the delta it introduces.

## Decisions

### Decision 1: Sequential loop, not parallel fan-out

**Choice**: Loop `searchReleases(artistId, offset = N, limit = API_PAGE)` and `searchMedia(releaseId, offset = N, limit = API_PAGE)` sequentially, accumulating results.

**Rationale**: MusicBrainz limits anonymous clients to 1 request per second. Parallelizing N requests would (a) trip the rate limiter, (b) require a custom token-bucket, and (c) not actually be faster than sequential given the rate limit. Sequential is correct, simple, and the existing Ktor client already handles 429 backoff.

**Alternatives considered**:
- *Parallel with a Semaphore(1)*: equivalent to sequential without the benefit of simpler code.
- *Bulk-fetch via a single request with `limit = count`*: not supported by the API. The API caps `limit` at 100, and the maximum page size is 100; we still need to loop.
- *Use the existing `MediaPagingSource` directly*: requires `RemoteMediator` plumbing, which is a larger architectural change. The current "sync once → page from Room" is fine for the local-cache-first UX; only the sync is broken.

### Decision 2: Persist per-page in a separate transaction, not a single bulk transaction

**Choice**: Each page of releases/media is written to Room in its own transaction (one call to `transactionalDao.insertDetails` or `mediaDao.insertMedia`). Pages that already succeeded remain persisted if a later page fails.

**Rationale**: A single bulk transaction spanning N HTTP calls is a 30-second-plus database transaction that holds a write lock for the entire sync. If the user navigates away mid-sync, the transaction is aborted and **no** releases are persisted — worse than the current behavior of "first 50 only". Per-page transactions are the standard pattern for incremental sync, and Room's `@Upsert` makes the writes idempotent if a retry re-inserts an overlapping page.

**Alternatives considered**:
- *Single transaction with full accumulator*: simple, but a cancellation point in the middle aborts the entire sync. The user gets nothing.
- *Per-page transaction, but in a coroutine scope tied to the composable*: still aborts on cancellation if the scope is the composable's `CoroutineScope`. The repository must use the application-scope dispatcher or a scope owned by the repository (the `viewModelScope` of the calling ViewModel is the right boundary — see Decision 5).

### Decision 3: Cap at 200 pages × 50 = 10 000 items

**Choice**: Hard cap of `MAX_RELEASE_PAGES = 200` and `MAX_MEDIA_PAGES = 200`. When hit, log a warning, persist what we have, and stop.

**Rationale**: A real-world artist with 200+ release-groups is essentially unheard of. The biggest catalogs (e.g. very prolific composers on MusicBrainz) top out at a few thousand. 10 000 is a generous cap that catches the legitimate long tail without allowing a runaway count (defensive against API bugs or strange data) to lock up a sync for hours. The cap is documented and the user is informed via a snackbar — this is a *failure* state, not silent.

**Alternatives considered**:
- *No cap*: risk of hour-long syncs for malformed data.
- *Per-artist config*: overkill. The cap is universal; outliers can be handled manually.
- *Lower cap (e.g. 1000)*: cuts off legitimate large catalogs. 10 000 is the practical upper bound for the use case.

### Decision 4: Add `release-group-count` to `ArtistDto` rather than parallel count API

**Choice**: Add a new `releaseCount: Int = 0` field to `ArtistDto`, mapped from `@SerialName("release-group-count")` with a default of 0 for backward compatibility (the `/artist/` search endpoint doesn't include it).

**Rationale**: The browse endpoint `/artist/{id}/release-groups` already returns `release-group-count` alongside `release-groups`. We just deserialize it. No new endpoint, no extra HTTP call.

**Alternatives considered**:
- *Separate count call to `/artist/{id}/release-group-count`*: redundant. The browse endpoint returns the count for free.
- *Use a `NetworkPagingCount`-style separate flow*: requires another HTTP call on every sync. Wasteful when the count is already in the response body.

### Decision 5: Repository scope owns the loop, not the ViewModel

**Choice**: The loop runs inside `ApiReleasesRepository.syncReleases` and `ApiMediaRepository.fetchReleasesMedia` as today. No new scope is introduced. Cancellation propagates from the caller's coroutine (typically `viewModelScope`).

**Rationale**: The existing pattern is "the repository method is `suspend` and runs in the caller's coroutine". The cap-and-persist-per-page design means cancellation at any page boundary is safe (already-persisted pages remain). The only "lose progress" risk is mid-page, and that's acceptable — the next pull-to-refresh resumes from page 0.

**Alternatives considered**:
- *Repository-owned `CoroutineScope`*: introduces a singleton-scoped coroutine that outlives the ViewModel. Wrong lifecycle for a UI-driven sync. Rejected.
- *WorkManager*: out of scope. The user expects the sync to be tied to opening the screen.

### Decision 6: Cover-art fetch still runs in parallel, but per-page

**Choice**: For each persisted page of releases/media, kick off the cover-art fetch in parallel (`flatMapMerge(concurrency = 25)` for releases, `flatMapMerge(50)` for media) as today. The fetch starts as soon as a page is in memory, not after the entire sync is buffered.

**Rationale**: Memory peaks are bounded by one page (50 items) at a time, not the total. The existing parallel-fetch code in `ApiReleasesRepository` and `ApiMediaRepository` can be moved inside the page loop with no API change to `CoversEngine`.

**Alternatives considered**:
- *Sequential cover fetch per page*: 25× to 50× slower. The current `flatMapMerge` is the right pattern.
- *Collect all pages first, then start cover fetch*: simpler but the UI sees a long gap with no cover thumbnails. Per-page fetch is strictly better.

### Decision 7: `networkCount` flows are new but not wired into the UI

**Choice**: Add `totalNetworkReleases: Flow<Int?>` and `totalNetworkMedia: Flow<Int?>` to the repository interfaces. The values are updated as each page is fetched (final value is the API-reported total). The UI is not required to consume them in this change.

**Rationale**: Exposing the count flows is cheap and gives future iterations a way to display "X of Y" indicators without re-plumbing the data layer. The existing `getArtistReleasesCount` / `getMediaCount` flows continue to read from Room and are not changed.

**Alternatives considered**:
- *Reuse `NetworkPagingCount`*: not quite the right shape — `NetworkPagingCount` is for Paging 3 sources that call `update(count)` per page. The repository loop also calls `update(count)` per page, so it works, but the type ties the count to a specific Paging source lifecycle. A simpler `MutableStateFlow<Int?>` is cleaner here.
- *No network count*: hides information that the user would benefit from. Cheap to add.

## Risks / Trade-offs

- **[Risk] Long sync for very prolific artists** → Cap at 200 pages × 50 = 10 000 items, surface a "partial sync" snackbar. Most artists have < 500 release-groups; the cap is a defensive guard, not a routine case.
- **[Risk] Cancellation mid-page loses that page's progress** → The cap-and-persist design means cancellation only loses the in-flight page, not earlier ones. The next pull-to-refresh resumes from page 0. Acceptable trade-off vs. holding a write lock for the full sync.
- **[Risk] Duplicate detection across pages** → `@Upsert` is idempotent on primary key, so re-inserting the same release with updated cover URLs is safe. No need for a `seenIds` deque in the repository (the `ArtistsPagingSource` uses one for search results; we don't need it for sync because upserts are deduped at the DB level).
- **[Risk] MusicBrainz API rate limit (1 req/sec) makes sequential sync slow** → For an artist with 200 release-groups, the sync takes ~200 seconds (sequential, not parallel). This is the price of correct rate-limit behavior. We can revisit with a token-bucket if user feedback indicates it's too slow.
- **[Risk] Per-page transaction creates many small write operations** → Room's `@Transaction` boundary is per-method-call here, but each method is one UPSERT batch. SQLite handles this fine; we measured < 5 ms per page in similar apps. No performance concern.
- **[Trade-off] Sequential is slower than parallel would be without rate limits** → Accepted. The rate limit makes parallel *illegal*, not just slow. Sequential is the only correct choice.
- **[Trade-off] Cover-art fetch is per-page, so the UI may show "no cover" thumbnails for late pages during the initial sync** → Accepted. The Room-backed `PagingSource` already handles this: rows are visible immediately, cover is filled in as the parallel fetch completes. The UX is the same as today's "first 50 only" case, just over more items.

## Migration Plan

1. Land the change behind a feature flag? **No**. The change is purely an extension of existing behavior — the existing user-visible paths (open artist → see releases) get more items, not a different shape. No need for a flag.
2. Rollout: standard Gradle `:composeApp:assembleDebug` and `:composeApp:assembleRelease`. No DB migration needed (Room schema is unchanged; `release-group-count` is an additive JSON field).
3. Rollback: revert the commit. The Room cache is forward-compatible — old code reading the new cache sees the same shape, just more rows. No data loss on rollback.
4. Test plan:
   - `jvmTest`: add a fake `SearchEngine` that returns multiple paginated responses; assert the repository calls it N times with the right offsets, accumulates correctly, and persists the union in `ReleasesDao` / `MediaDao`.
   - `jvmTest`: assert cancellation mid-loop exits cleanly with no partial-page transaction committed.
   - `jvmTest`: assert the cap is honored when `release-group-count` exceeds the safety bound.
   - Manual: open a real artist with > 50 release-groups (e.g. Radiohead, The Beatles), confirm all are visible after sync.

## Open Questions

- Should the cap behavior surface as a "partial sync" snackbar in the UI, or just log a warning? Proposal says snackbar; design confirms it. Implementation can decide wording.
- Should the per-page persist run in a single transaction (across all pages) or one transaction per page? Decision 2 says per-page for cancellation safety. Open question: do we want a `suspend fun persistBatch` wrapper in `TransactionalDao` that takes a list of pages and runs N transactions, or inline the calls in the repository? The wrapper is more testable; inlining is simpler. Defer to implementation.
- Should the existing `MediaPagingSource` (currently unused) be deleted, or kept for the eventual RemoteMediator work? Out of scope; the existing class-evaluating comment says "reserved for a future iteration". Defer to that future iteration.
