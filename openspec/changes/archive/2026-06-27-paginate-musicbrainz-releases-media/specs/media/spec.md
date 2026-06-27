# Media Capability

## Purpose

The media screen shows a paginated list of media (tracks) belonging to a single release. Media are loaded from the local Room database and refreshed from the MusicBrainz API on first open or on user request. The network sync reads **all** tracks for the release (up to a safety cap), not just the first page.

## ADDED Requirements

### Requirement: Media are paged

The system MUST load a release's media via a `Flow<PagingData<Media>>` returned by `MediaRepository.getReleaseMedia(releaseId)`. The page size MUST be `PagingDefaults.ROOM_PAGE` (20). The first page MUST load automatically when the screen is first displayed. The next page MUST load automatically when the user scrolls within `PagingDefaults.PREFETCH` (20) items of the end of the loaded list.

#### Scenario: First page loads on open

- **WHEN** the user opens a release's media list
- **THEN** the first 20 tracks (or all tracks, whichever is smaller) are visible

#### Scenario: User scrolls to trigger the next page

- **WHEN** the user scrolls so that fewer than 20 items remain below the viewport
- **THEN** the next page of tracks is appended to the list

### Requirement: Initial sync reads all media from network

The system MUST trigger a one-shot network sync when the local Room cache for the release is empty. The sync MUST page through the MusicBrainz `/release/` endpoint with `release-group={id}` and `inc=media+recordings+url-rels` using `offset` and `limit` parameters until `offset >= response.count` or the response page is empty. The page size MUST be `PagingDefaults.API_PAGE` (50).

#### Scenario: First open with empty cache and short release

- **WHEN** the user opens a release's media list and the Room cache has no entries for that release
- **AND** the response `count` is 8
- **THEN** the system issues exactly one `searchMedia(releaseId, offset = 0, limit = 50)` request
- **AND** stores the 8 media, their items, tracks, and resources in Room
- **AND** the first page of the now-populated cache is visible

#### Scenario: First open with empty cache and long release

- **WHEN** the user opens a release's media list and the Room cache has no entries for that release
- **AND** the response `count` is 124
- **THEN** the system issues three paginated requests with offsets 0, 50, 100 and limits of 50
- **AND** accumulates 124 media across the three responses
- **AND** stores the union of media, items, tracks, and resources in Room

#### Scenario: Subsequent open with populated cache

- **WHEN** the user opens a release's media list and the Room cache has at least one entry for that release
- **THEN** the system does NOT call `searchMedia(releaseId)` automatically

#### Scenario: Partial sync on safety cap

- **WHEN** the sync reaches the safety cap of `MAX_MEDIA_PAGES` × `API_PAGE` items (10 000)
- **AND** `response.count` is greater than the cap
- **THEN** the system persists the items fetched so far
- **AND** surfaces a "partial sync" indicator to the user
- **AND** does NOT continue looping past the cap

### Requirement: Refresh errors preserve existing data

The system MUST keep the existing paged list visible if a network sync fails after the cache is already populated. The system MUST surface the error to the user via a Snackbar message.

#### Scenario: Network sync fails with existing data

- **WHEN** the user triggers a refresh and a `searchMedia` page returns an error
- **THEN** the existing paged list is still visible
- **AND** a Snackbar message describing the error is shown
- **AND** the list is NOT replaced with an error placeholder
- **AND** any pages already persisted in this sync attempt remain in Room

#### Scenario: Network sync fails with empty cache

- **WHEN** the user opens the media list and the first `searchMedia` page returns an error
- **THEN** the system shows an error placeholder
- **AND** the error placeholder offers a "Retry" action that re-invokes the sync from offset 0

#### Scenario: Network sync fails mid-loop

- **WHEN** the sync has persisted the first 50 tracks and the next page returns an error
- **THEN** the first 50 tracks are visible
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
- **AND** no further `searchMedia` requests are issued
- **AND** any pages already persisted in Room remain
