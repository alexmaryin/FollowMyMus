# Releases Capability

## Purpose

The releases screen shows a paginated, grouped list of music releases (albums, singles, EPs, etc.) for a single artist. Releases are loaded from the local Room database and refreshed from the MusicBrainz API on first open or on user request.

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

### Requirement: Initial sync loads from network on empty cache

The system MUST trigger a one-shot network sync of releases (`searchReleases(artistId)`) when the local Room cache for the artist is empty.

#### Scenario: First open with empty cache

- **WHEN** the user opens an artist's releases and the Room cache has no entries for that artist
- **THEN** the system calls `searchReleases(artistId)` to fetch releases from the MusicBrainz API
- **AND** the system stores the API response in Room
- **AND** the system fetches cover art for each release in the background
- **AND** the first page of the now-populated Room cache is visible in the list

#### Scenario: Subsequent open with populated cache

- **WHEN** the user opens an artist's releases and the Room cache has at least one entry for that artist
- **THEN** the system does NOT call `searchReleases(artistId)` automatically
- **AND** the cached releases are visible

### Requirement: Pull-to-refresh re-fetches from network

The system MUST re-trigger the network sync when the user explicitly invokes pull-to-refresh.

#### Scenario: User pulls to refresh

- **WHEN** the user performs a pull-to-refresh gesture on the releases list
- **THEN** the system calls `searchReleases(artistId)`
- **AND** the system shows a refresh indicator while the sync is in progress
- **AND** the list reflects the new data after the sync completes

### Requirement: Refresh errors preserve existing data

The system MUST keep the existing paged list visible if a network sync fails after the cache is already populated. The system MUST surface the error to the user via a Snackbar message tied to the artist id.

#### Scenario: Network sync fails with existing data

- **WHEN** the user pulls to refresh and `searchReleases(artistId)` returns an error
- **THEN** the existing paged list is still visible
- **AND** a Snackbar message describing the error is shown
- **AND** the list is NOT replaced with an error placeholder

#### Scenario: Network sync fails with empty cache

- **WHEN** the user opens the releases screen for the first time and `searchReleases(artistId)` returns an error
- **THEN** the system shows an error placeholder in the list area
- **AND** the error placeholder offers a "Retry" action that re-invokes the sync

### Requirement: Append errors show a footer with retry

The system MUST render an inline error footer with a "Retry" button when an append page load fails.

#### Scenario: Append load fails

- **WHEN** the user scrolls near the end of the list and the next page load fails
- **THEN** a row appears at the bottom of the list with the error message and a "Retry" button
- **AND** pressing the button retries the failed page load
