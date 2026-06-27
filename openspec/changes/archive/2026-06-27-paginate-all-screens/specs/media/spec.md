# Media Capability

## Purpose

The media screen shows a paginated list of media (tracks) belonging to a single release. Media are loaded from the local Room database and refreshed from the MusicBrainz API on first open or on user request.

## ADDED Requirements

### Requirement: Media are paged

The system MUST load a release's media via a `Flow<PagingData<Media>>` returned by `MediaRepository.getReleaseMedia(releaseId)`. The page size MUST be `PagingDefaults.ROOM_PAGE` (20). The first page MUST load automatically when the screen is first displayed. The next page MUST load automatically when the user scrolls within `PagingDefaults.PREFETCH` (20) items of the end of the loaded list.

#### Scenario: First page loads on open

- **WHEN** the user opens a release's media list
- **THEN** the first 20 tracks (or all tracks, whichever is smaller) are visible

#### Scenario: User scrolls to trigger the next page

- **WHEN** the user scrolls so that fewer than 20 items remain below the viewport
- **THEN** the next page of tracks is appended to the list

### Requirement: Initial sync loads from network on empty cache

The system MUST trigger a one-shot network sync (`searchMedia(releaseId)`) when the local Room cache for the release is empty.

#### Scenario: First open with empty cache

- **WHEN** the user opens a release's media list and the Room cache has no entries for that release
- **THEN** the system calls `searchMedia(releaseId)` to fetch the media
- **AND** the system stores the response in Room
- **AND** the first page of the now-populated cache is visible

#### Scenario: Subsequent open with populated cache

- **WHEN** the user opens a release's media list and the Room cache has at least one entry for that release
- **THEN** the system does NOT call `searchMedia(releaseId)` automatically

### Requirement: Refresh errors preserve existing data

The system MUST keep the existing paged list visible if a network sync fails after the cache is already populated. The system MUST surface the error to the user via a Snackbar message.

#### Scenario: Network sync fails with existing data

- **WHEN** the user triggers a refresh and `searchMedia(releaseId)` returns an error
- **THEN** the existing paged list is still visible
- **AND** a Snackbar message describing the error is shown
- **AND** the list is NOT replaced with an error placeholder

#### Scenario: Network sync fails with empty cache

- **WHEN** the user opens the media list and `searchMedia(releaseId)` returns an error
- **THEN** the system shows an error placeholder
- **AND** the error placeholder offers a "Retry" action

### Requirement: Append errors show a footer with retry

The system MUST render an inline error footer with a "Retry" button when an append page load fails.

#### Scenario: Append load fails

- **WHEN** the user scrolls near the end of the list and the next page load fails
- **THEN** a row appears at the bottom of the list with the error message and a "Retry" button
- **AND** pressing the button retries the failed page load

### Requirement: Page size for network fallback

The system MUST use `PagingDefaults.API_PAGE` (50) as the page size when fetching the initial sync from the MusicBrainz API. The system MUST use `PagingDefaults.ROOM_PAGE` (20) as the page size when reading from the local Room cache.

#### Scenario: Initial sync uses larger page

- **WHEN** the system calls `searchMedia(releaseId)` to populate the empty cache
- **THEN** the request asks for up to 50 tracks in a single response
- **AND** the response is split into Room rows but the UI page size remains 20
