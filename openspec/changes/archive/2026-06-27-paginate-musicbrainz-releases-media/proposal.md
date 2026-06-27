## Why

`ApiReleasesRepository.syncReleases` and `ApiMediaRepository.fetchReleasesMedia` read only the first page of the MusicBrainz API responses — a single batch of up to 50 items per call. Both `SearchEngine.searchReleases` and `SearchEngine.searchMedia` already accept `offset` and `limit` parameters, and `MediaPagingSource` is already implemented for media, but neither repository loops through pages. As a result, the local Room cache is under-populated: an artist with 200 release-groups is stored with only the first 50, and a release with 120 tracks is stored with only the first 50. The user sees a complete-feeling list that is actually a truncated slice. The existing paging infrastructure (Paging 3, Room `PagingSource`, `PagingDefaults`) is fine; the gap is solely in the network-sync code path.

## What Changes

- **Add `release-group-count` to `ArtistDto`** so the repository can know when to stop looping on the releases browse endpoint. Map it to a new `releaseCount: Int` field with a default of `0` for backward compatibility (the `/artist/` search endpoint that hits `SearchArtistResponse` does not include it).

- **Paginate the releases network sync** in `ApiReleasesRepository.syncReleases`:
  - Loop `searchReleases(artistId, offset = N, limit = API_PAGE)` until `offset >= artistDto.releaseCount` or the response page is empty.
  - Accumulate releases and resources across all pages.
  - Persist the whole batch in a single `transactionalDao.insertDetails(...)` call (or chunked transactions if the batch is large).
  - Update `networkCount` (or a similar `ReleasesPagingCount` helper) so the UI can display a total-count and so `getArtistReleasesCount` returns the real network total, not just the cached count.
  - Stream each fetched release through `coversEngine.getReleaseCovers` as before; ordering by `firstReleaseDate DESC` is preserved across the union of pages.

- **Paginate the media network sync** in `ApiMediaRepository.fetchReleasesMedia`:
  - Loop `searchMedia(releaseId, offset = N, limit = API_PAGE)` until `offset >= response.count` or the response page is empty.
  - Accumulate `media`, `items`, `tracks`, and `mediaResources` across all pages.
  - Persist the union in a single `mediaDao.insertMedia(...)` call (or chunked transactions if the batch is large).
  - Update the media count helper so `getMediaCount` reflects the real network total.

- **Surface the total network count** alongside the cached count. Today `getArtistReleasesCount` only reads from Room; add a network count tracker similar to `NetworkPagingCount` so the UI can show "127 releases" while the sync is running, then collapse to the Room count once the sync completes.

- **Cancel gracefully** on `Error` mid-loop. If any page fails, stop the loop, surface the error via `_errors`, and keep whatever has been written so far (no rollback — the user can pull-to-refresh to retry the missing pages). This matches the "refresh errors preserve existing data" semantics already in the `releases` and `media` specs.

- **Cap the safety bound** at `MusicBrainz.MAX_RELEASE_PAGES` × `API_PAGE` (e.g. 200 pages × 50 = 10 000 items) to defend against an artist with a runaway `release-group-count` (MusicBrainz has historical entries with tens of thousands of release-groups for prolific composers). When the cap is hit, log a warning, persist what we have, and surface a "partial sync" snackbar.

- **Honor cancellation** — if the composable scope is cancelled mid-loop, exit cleanly without writing a partial page transaction.

## Capabilities

### New Capabilities

None. The behavior change is purely about the network sync code path inside the existing `releases` and `media` capabilities.

### Modified Capabilities

- `releases`: extend the "Initial sync loads from network on empty cache" requirement to require that the sync reads **all** release-groups, not just the first 50. Add a "Partial sync on cap" scenario.

- `media`: extend the "Initial sync loads from network on empty cache" requirement to require that the sync reads **all** tracks, not just the first 50. Add a "Partial sync on cap" scenario.

## Impact

- **Code**:
  - Update: `musicBrainz/data/remote/model/ArtistDto.kt` — add `releaseCount: Int = 0` mapped from `@SerialName("release-group-count")`.
  - Update: `musicBrainz/data/repository/ApiReleasesRepository.kt` — replace the single `searchReleases` call with a paginated loop. Move cover-art streaming inside the per-page collector.
  - Update: `musicBrainz/data/repository/ApiMediaRepository.kt` — replace the single `searchMedia` call with a paginated loop. Move cover-art streaming inside the per-page collector.
  - Update: `musicBrainz/data/local/dao/TransactionalDao.kt` and `musicBrainz/data/local/dao/MediaDao.kt` — if the current single-call insert cannot accept a list larger than the existing page, accept it as a list and chunk inside (no contract change for callers).
  - Update: `musicBrainz/domain/ReleasesRepository.kt` and `musicBrainz/domain/MediaRepository.kt` — the `count` flows are still `Flow<Int?>`; the underlying provider changes from "Room-only" to "Room + network" without altering the type.

- **Network**: a single artist sync may now issue N HTTP requests instead of 1. For a typical artist with 50–200 release-groups this is 1–4 requests; for a prolific composer it can be 10+. The MusicBrainz rate limit (1 req/sec for anonymous clients) is respected by Ktor's existing retry/backoff. No new dependency on the request rate — we are not adding parallelism, just sequential pagination.

- **DB**: each new page appends rows in the same transaction pattern as today. The Room schema is unchanged.

- **Tests**: `jvmTest` fakes for `SearchEngine.searchReleases` / `searchMedia` need to support multiple calls. Add a fake that returns a paginated response so the loop is exercised.

- **UI**: no change. The `LazyPagingItems` flow from Room is unchanged; once the sync completes, the Room-backed `PagingSource` sees more rows and emits them naturally. A `networkCount` is exposed but not yet wired into the UI; that's a follow-up.

- **Performance**: the sync is now linear in `releaseCount / API_PAGE` for releases and `trackCount / API_PAGE` for media. Cover-art fetching is still parallel (concurrency = 25 / 50), but it now only begins after a full page is in memory, not after the entire sync is buffered. Memory peaks are bounded by `API_PAGE` (50) per page, not the total.
