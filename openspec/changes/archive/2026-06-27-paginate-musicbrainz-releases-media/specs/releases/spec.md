# Releases Capability

## Purpose

The releases screen shows a paginated, grouped list of music releases (albums, singles, EPs, etc.) for a single artist. Releases are loaded from the local Room database and refreshed from the MusicBrainz API on first open or on user request. The network sync reads **all** release-groups for the artist (up to a safety cap), not just the first page.

## ADDED Requirements

### Requirement: Releases are paged

The system MUST load an artist's releases via a `Flow<PagingData<Release>>` returned by `ReleasesRepository.getArtistReleases(artistId)`. The page size MUST be `PagingDefaults.ROOM_PAGE` (20). The first page MUST load automatically when the screen is first displayed. The next page MUST load automatically when the user scrolls within `PagingDefaults.PREFETCH` (20) items of the end of the loaded list.

#### Scenario: First page loads on open

- **WHEN** the user opens an artist's releases for the first time
- **THEN** the system loads the first 20 releases (or all releases, whichever is smaller)
- **AND** they are visible in the list

#### Scenario: User scrolls to trigger the next page

- **WHEN** the user scrolls so that fewer than 20 items remain below the viewport
- **THEN** the system loads the next page of releases
- **AND** the new releases are appended to the list without scroll position reset

#### Scenario: All releases have been loaded

- **WHEN** the user has scrolled to the end of the last page
- **THEN** no further network or database load is triggered
- **AND** the list footer shows an "end of list" indicator (or remains blank, per the caller's design)

### Requirement: Releases are grouped by year bucket

The system MUST group the paged releases by year bucket via the `PagingData<T>.groupedBy` extension. The default key selector MUST be `{ release -> release.firstReleaseDate?.year ?: 0 }`. Each group MUST be preceded by a sticky (or in-flow, per the caller's design) header showing the year.

#### Scenario: Releases span multiple years

- **WHEN** the loaded releases include items from 2023, 2022, and 2020
- **THEN** the list shows a "2023" header before the first 2023 release, a "2022" header before the first 2022 release, and a "2020" header before the first 2020 release

#### Scenario: Some releases have no release date

- **WHEN** some releases in the list have a `null` `firstReleaseDate`
- **THEN** they are grouped under a "0" header (or "Unknown" if the caller maps it)

### Requirement: Initial sync reads all release-groups from network

The system MUST trigger a one-shot network sync of releases when the local Room cache for the artist is empty. The sync MUST page through the MusicBrainz `/artist/{id}/release-groups` endpoint using `offset` and `limit` parameters until the total reported by `release-group-count` has been fetched or the response page is empty. The page size MUST be `PagingDefaults.API_PAGE` (50).

#### Scenario: First open with empty cache and small catalog

- **WHEN** the user opens an artist's releases and the Room cache has no entries for that artist
- **AND** the artist has 30 release-groups according to `release-group-count`
- **THEN** the system issues exactly one `searchReleases(artistId, offset = 0, limit = 50)` request
- **AND** stores the 30 releases in Room
- **AND** fetches cover art for each release in the background
- **AND** the first page of the now-populated Room cache is visible in the list

#### Scenario: First open with empty cache and large catalog

- **WHEN** the user opens an artist's releases and the Room cache has no entries for that artist
- **AND** the artist has 137 release-groups according to `release-group-count`
- **THEN** the system issues three paginated requests with offsets 0, 50, 100 and limits of 50
- **AND** accumulates 137 releases across the three responses
- **AND** stores the union in Room
- **AND** fetches cover art for each release in the background

#### Scenario: Subsequent open with populated cache

- **WHEN** the user opens an artist's releases and the Room cache has at least one entry for that artist
- **THEN** the system does NOT call `searchReleases(artistId)` automatically
- **AND** the cached releases are visible

#### Scenario: Pull-to-refresh re-paginates from network

- **WHEN** the user performs a pull-to-refresh gesture on the releases list
- **THEN** the system re-paginates `searchReleases(artistId, offset = N, limit = API_PAGE)` from offset 0
- **AND** the system shows a refresh indicator while the sync is in progress
- **AND** the list reflects the new data after the sync completes

#### Scenario: Partial sync on safety cap

- **WHEN** the sync reaches the safety cap of `MAX_RELEASE_PAGES` × `API_PAGE` items (10 000)
- **AND** `release-group-count` is greater than the cap
- **THEN** the system persists the items fetched so far
- **AND** surfaces a "partial sync" indicator (snackbar or banner) to the user
- **AND** does NOT continue looping past the cap

### Requirement: Refresh errors preserve existing data

The system MUST keep the existing paged list visible if a network sync fails after the cache is already populated. The system MUST surface the error to the user via a Snackbar message tied to the artist id.

#### Scenario: Network sync fails with existing data

- **WHEN** the user pulls to refresh and a `searchReleases` page returns an error
- **THEN** the existing paged list is still visible
- **AND** a Snackbar message describing the error is shown
- **AND** the list is NOT replaced with an error placeholder
- **AND** any pages that were already persisted in this sync attempt remain in Room

#### Scenario: Network sync fails with empty cache

- **WHEN** the user opens the releases screen for the first time and the first `searchReleases` page returns an error
- **THEN** the system shows an error placeholder in the list area
- **AND** the error placeholder offers a "Retry" action that re-invokes the sync from offset 0

#### Scenario: Network sync fails mid-loop

- **WHEN** the sync has persisted the first 50 release-groups and the next page returns an error
- **THEN** the first 50 release-groups are visible in the list
- **AND** a Snackbar indicates the sync was incomplete
- **AND** the user can pull-to-refresh to retry from the next offset (or from 0)

### Requirement: Append errors show a footer with retry

The system MUST render an inline error footer with a "Retry" button when an append page load fails.

#### Scenario: Append load fails

- **WHEN** the user scrolls near the end of the list and the next page load fails
- **THEN** a row appears at the bottom of the list with the error message and a "Retry" button
- **AND** pressing the button retries the failed page load

### Requirement: Sync honors cancellation

The system MUST exit the network-sync loop cleanly when the calling coroutine is cancelled. The system MUST NOT persist a partial page transaction after cancellation.

#### Scenario: User navigates away mid-sync

- **WHEN** the user navigates to a different screen while the sync loop is in progress
- **THEN** the loop exits at the next suspension point
- **AND** no further `searchReleases` requests are issued
- **AND** any pages already persisted in Room remain
