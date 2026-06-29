# New Releases Capability

## Purpose

The New Releases page surfaces release-groups from the user's favorite artists released within a recent window. Each row carries a three-state lifecycle (UNSEEN, SEEN, DISMISSED) so the user can tell what's new, what they've already opened, and what they've explicitly hidden. The page is the third tab in the main pager (`PagerConfig.Releases`), and is empty for users with no favorites.

The page's two app-level settings (the sync floor and the "last updated" timestamp) are stored in DataStore preferences — not in the Room database — and follow the same `Flow<T>` getter / `suspend` setter pattern as the existing theme, language, and dynamic-mode settings.

## Requirements

### Requirement: App settings live in DataStore preferences, not in a Room table

The system MUST store the New Releases app settings in the existing DataStore-backed `PreferenceSource` as two `stringPreferencesKey` entries — `new_releases_last_opened_day` and `new_releases_last_sync_completed_at` — rather than in a Room entity or DAO. The values MUST be ISO-8601 strings (`LocalDate.toString()` for the floor, `Instant.toString()` for the timestamp). The system MUST expose the settings as a `Flow<AppSettings>` from `PreferenceSource.getAppSettings()` and as a `suspend fun setNewReleasesFloor(day: LocalDate, now: Instant)` that updates both keys atomically in a single `prefs.edit { }` block. The settings file is `.preferences_pb`, distinct from the Room database file.

The settings MUST survive the v1→v2 destructive Room migration described below, because they are stored in a separate file.

The system MUST NOT add an `app_settings` entity, an `AppSettingsDao`, or any Room column for the floor or the last-sync timestamp. Writing a setting MUST go through the DataStore extension, not through the Room database.

#### Scenario: AppSettings has no Room presence

- **WHEN** the Room database is inspected at v2
- **THEN** there is no `app_settings` table
- **AND** there is no `AppSettingsDao` registered
- **AND** the new-releases settings live only in `.preferences_pb`

#### Scenario: First sync ever — no DataStore floor

- **WHEN** the user opens the New Releases page for the first time
- **AND** `PreferenceSource.getAppSettings().first().newReleasesLastOpenedDay` is `null`
- **THEN** the system fetches all release-groups with `first-release-date >= today - 30 days`
- **AND** persists them in the `new_releases` table with state `UNSEEN`

#### Scenario: Subsequent sync — DataStore floor is set

- **WHEN** the user opens the New Releases page
- **AND** `PreferenceSource.getAppSettings().first().newReleasesLastOpenedDay` is set to a date D
- **THEN** the system fetches all release-groups with `first-release-date > D`
- **AND** on successful completion calls `PreferenceSource.setNewReleasesFloor(today, Clock.System.now())` — a single `prefs.edit { }` that updates both keys atomically

#### Scenario: Settings survive a destructive Room migration

- **WHEN** the v1→v2 destructive migration runs on open
- **THEN** the Room database is reset
- **AND** the DataStore `.preferences_pb` is unaffected
- **AND** the next sync uses the previously-persisted `newReleasesLastOpenedDay` (if any)

#### Scenario: Atomic write of the floor and the timestamp

- **WHEN** the sync completes successfully and the repository calls `setNewReleasesFloor(day, now)`
- **THEN** the `prefs.edit { }` block is atomic — both keys are visible to subsequent `getAppSettings().first()` callers, or neither is (if the write fails)
- **AND** no caller ever observes a half-updated state (floor updated, timestamp not yet, or vice versa)

### Requirement: New Releases page is in the main pager

The system MUST provide a New Releases page that occupies the `PagerConfig.Releases` slot in the main pager. The page MUST be reachable via the bottom navigation bar. The page MUST display a list of release-groups from the user's favorite artists, sorted by `first-release-date` descending.

#### Scenario: User opens the New Releases page

- **WHEN** the user taps the Releases tab in the bottom navigation
- **THEN** the system displays the New Releases page
- **AND** starts a sync of new releases in the background

#### Scenario: User has no favorites

- **WHEN** the user opens the New Releases page
- **AND** the favorites list is empty
- **THEN** the system displays an empty state with a "follow some artists to see new releases" message
- **AND** does not call the MusicBrainz search endpoint

### Requirement: New releases are auto-synced on page open

The system MUST trigger a sync of new releases when the New Releases page is opened. The sync MUST be triggered via `lifecycle.doOnStart` of the host, mirroring the pattern used by `FavoritesHost` and `ArtistsHost`. The sync MUST run in the host's `viewModelScope` and MUST honor coroutine cancellation when the user navigates away.

The system MUST read the sync floor by reading the `Flow<AppSettings>` from `PreferenceSource.getAppSettings().first()` — NOT by querying any Room table. The floor is `today - 30 days` on the first sync (when the floor is `null`) and `floor + 1 day` on every subsequent sync.

#### Scenario: First ever sync

- **WHEN** the user opens the New Releases page for the first time
- **AND** the DataStore-backed `AppSettings.newReleasesLastOpenedDay` is `null`
- **THEN** the system fetches all release-groups from the user's favorite artists with `first-release-date >= today - 30 days`
- **AND** persists them in the `new_releases` table with state `UNSEEN`

#### Scenario: Subsequent sync

- **WHEN** the user opens the New Releases page
- **AND** the DataStore-backed `AppSettings.newReleasesLastOpenedDay` is set to a date D
- **THEN** the system fetches all release-groups from the user's favorite artists with `first-release-date > D`
- **AND** on successful completion calls `setNewReleasesFloor(today, now)` to update the DataStore-backed `AppSettings`

#### Scenario: Sync runs in background

- **WHEN** a sync is running
- **AND** the user scrolls the list
- **THEN** the list is populated with results from already-completed batches
- **AND** the user can interact with the list (scroll, open releases) while later batches are still being fetched

### Requirement: New releases are fetched in batches respecting MusicBrainz rate limits

The system MUST fetch new releases in batches of 50 favorite-artist ids per request. Each batch MUST be sent as a single `GET /ws/2/release-group?query=firstreleasedate:[startDay TO today] AND arid:(id1 OR id2 OR ... OR idN)&limit=100&offset=M&fmt=json` request. Note: the search-index field is `firstreleasedate` (no hyphens); the response field on the wire is `first-release-date` (kebab-case) — only the unhyphenated form is legal in the `query=` parameter. The system MUST paginate within a batch at `limit=100` until `offset >= count` or the response page is empty or the cap of 200 pages is reached.

#### Scenario: User has 30 favorite artists

- **WHEN** the user has 30 favorite artists
- **THEN** the system issues exactly one search request per sync (50 ids ≤ batch size)
- **AND** iterates offset 0, 100, 200, ... until the response count is exhausted

#### Scenario: User has 500 favorite artists

- **WHEN** the user has 500 favorite artists
- **THEN** the system issues 10 search requests per sync (500 / 50 = 10 batches)
- **AND** serializes them through the rate-limited queue with at least 1 second between requests

#### Scenario: A batch returns more than 100 results

- **WHEN** a single batch query returns `count > 100`
- **THEN** the system pages through the batch with `offset=0, 100, 200, ...` until the count is exhausted
- **AND** the rate-limited queue spaces these page requests by 1 second as well

### Requirement: All MusicBrainz API calls go through the rate-limited queue

The system MUST route every MusicBrainz API call through a shared `RateLimitedApiQueue` singleton. The queue MUST enforce at least 1 second between any two MusicBrainz requests. The queue MUST be backed by a single-consumer coroutine reading from a `Channel<suspend () -> Unit>`, with a `delay(1000)` between jobs.

#### Scenario: Two consecutive API calls

- **WHEN** the repository issues two consecutive MusicBrainz API calls
- **THEN** the second call starts at least 1 second after the first call returns

#### Scenario: Existing repository is retrofitted

- **WHEN** `ApiReleasesRepository.syncReleases` issues a paginated loop of `searchReleases` calls
- **THEN** each call is wrapped in `rateLimitedApiQueue.enqueue { ... }`
- **AND** the loop's total wall-clock time is at least `(pageCount - 1) * 1000ms`

#### Scenario: Cancellation mid-queue

- **WHEN** the caller's coroutine is cancelled while a request is enqueued
- **THEN** the queue stops accepting new work for that caller
- **AND** the in-flight request is allowed to complete or is cancelled by the caller's scope

### Requirement: New releases have a three-state lifecycle

The system MUST track each row in `new_releases` with one of three states: `UNSEEN`, `SEEN`, or `DISMISSED`. A new release-group MUST be inserted with state `UNSEEN`. When the user opens the release (the media details panel is shown), the system MUST transition the row to `SEEN`. When the user swipes the row to dismiss, the system MUST transition the row to `DISMISSED`. The transition from `UNSEEN` to `SEEN` MUST be one-way. The transition from `SEEN` to `DISMISSED` MUST be allowed.

#### Scenario: New release is marked unseen

- **WHEN** a release-group is fetched for the first time
- **THEN** the row is inserted into `new_releases` with state `UNSEEN`

#### Scenario: User opens a release

- **WHEN** the user taps a release-group
- **AND** the media details panel opens for that release
- **THEN** the row's state is updated from `UNSEEN` to `SEEN`
- **AND** the dot indicator is no longer rendered for that row

#### Scenario: User swipes to dismiss

- **WHEN** the user swipes a release-group row to dismiss
- **THEN** the row's state is updated to `DISMISSED`
- **AND** the row is no longer rendered in the list

#### Scenario: User re-opens a previously seen release

- **WHEN** the user opens a release whose state is `SEEN`
- **THEN** the state remains `SEEN` (no transition back to `UNSEEN`)

### Requirement: Unseen releases render with a dot indicator and bold title

The system MUST render rows in state `UNSEEN` with a visible dot indicator and a bold title text style. Rows in state `SEEN` MUST be rendered without the dot and with a regular (non-bold) title text style. Rows in state `DISMISSED` MUST NOT be rendered in the list at all.

#### Scenario: Unseen row in the list

- **WHEN** the user views the New Releases page
- **AND** the list contains both UNSEEN and SEEN rows
- **THEN** UNSEEN rows display a dot and a bold title
- **AND** SEEN rows do not display a dot and have a regular title weight

#### Scenario: All releases are seen

- **WHEN** every release in the list has state `SEEN`
- **THEN** no dots are rendered
- **AND** the page still shows the list (it is not empty)

### Requirement: Re-syncing preserves existing release state

The system MUST upsert fetched release-groups into `new_releases` in a way that preserves the `state` column for any row that already exists. The system MUST update the mutable fields (title, disambiguation, cover URL, first-release-date) for existing rows, but MUST NOT overwrite the `state` column with the default value on a re-fetch.

#### Scenario: Re-fetch after user has seen a release

- **WHEN** a release is in state `SEEN` in the local table
- **AND** the same release is fetched again from MusicBrainz
- **THEN** the row's `state` remains `SEEN` (it is NOT reset to `UNSEEN`)

#### Scenario: Re-fetch after user has dismissed a release

- **WHEN** a release is in state `DISMISSED` in the local table
- **AND** the same release is fetched again from MusicBrainz
- **THEN** the row's `state` remains `DISMISSED`
- **AND** the row remains hidden from the list

#### Scenario: Newly fetched release-group

- **WHEN** a release-group is fetched that does not yet exist in the table
- **THEN** the row is inserted with state `UNSEEN`

### Requirement: New releases are grouped by artist with in-line headers

The system MUST group the list of new releases by `artistName` using the `PagingData<T>.groupedBy` extension. Headers MUST be rendered in-line (non-sticky), matching the existing per-artist releases panel. Each artist group MUST show the artist name as the header.

#### Scenario: Releases span multiple artists

- **WHEN** the loaded releases include items from three different artists
- **THEN** the list shows three artist-name headers, each followed by their release-groups
- **AND** the headers are in-line with the rest of the list (they scroll with the content)

### Requirement: Tap on a new release opens the shared media details panel

The system MUST open the shared `MediaDetails` panel when the user taps a new release. The `MediaDetails` factory MUST be a single shared function used by `FavoritesHost`, `ArtistsHost`, and `NewReleasesHost`. The shared `MediaPanelUi` MUST render the media grid and the track-list dialog.

#### Scenario: User taps a new release

- **WHEN** the user taps a release-group in the New Releases list
- **THEN** the system opens the `MediaDetails` panel for that release
- **AND** marks the release as `SEEN` in the local table

#### Scenario: User closes the media details panel

- **WHEN** the user closes the media details panel
- **THEN** the New Releases list is shown again
- **AND** the previously-opened release now has no dot indicator

### Requirement: Manual refresh re-triggers the sync

The system MUST provide a pull-to-refresh action that re-runs the sync from the current `newReleasesLastOpenedDay` to today. The refresh MUST use the same batched search and rate-limited queue as the auto-sync. The refresh MUST update the DataStore-backed `AppSettings` only on successful completion (by calling `setNewReleasesFloor`).

#### Scenario: User pulls to refresh

- **WHEN** the user pulls down on the New Releases list
- **THEN** the system re-runs the sync from the DataStore-backed `AppSettings.newReleasesLastOpenedDay` to today
- **AND** any newly-fetched release-groups are added to the list with state `UNSEEN`

### Requirement: Settings are written only on successful sync completion

The system MUST update the DataStore-backed `AppSettings.newReleasesLastOpenedDay` only after the entire sync has completed without error. If the sync ends in `PARTIAL_SYNC` state (any batch failed, or the 200-page cap was hit), the system MUST NOT update the floor — the next page-open re-attempts the same window. The system MUST update the DataStore-backed `AppSettings.newReleasesLastSyncCompletedAt` to the current time on successful completion. The two updates MUST happen in a single atomic `prefs.edit { }` block (i.e. via a single `setNewReleasesFloor` call).

#### Scenario: Full successful sync

- **WHEN** the sync completes without any batch error and the cap is not hit
- **THEN** `AppSettings.newReleasesLastOpenedDay` is updated to today
- **AND** `AppSettings.newReleasesLastSyncCompletedAt` is updated to the current instant
- **AND** both updates are visible in a single `getAppSettings().first()` snapshot after `setNewReleasesFloor` returns

#### Scenario: Partial sync due to a batch error

- **WHEN** one of the batches returns an error
- **THEN** the sync ends in `WorkState.PARTIAL_SYNC`
- **AND** `AppSettings.newReleasesLastOpenedDay` is NOT updated
- **AND** `AppSettings.newReleasesLastSyncCompletedAt` is NOT updated
- **AND** the user can see a "partial sync" snackbar

#### Scenario: Partial sync due to 200-page cap

- **WHEN** the per-batch pagination reaches the 200-page cap before exhausting the count
- **THEN** the sync ends in `WorkState.PARTIAL_SYNC`
- **AND** `AppSettings.newReleasesLastOpenedDay` is NOT updated
- **AND** the user can see a "partial sync" snackbar

### Requirement: Schema migration is destructive (new_releases only)

The system MUST bump the database version to 2 when the new `new_releases` table is added. The database builder MUST use `fallbackToDestructiveMigration` so that pre-existing v1 data is dropped on open. This is acceptable because the app is pre-release and the only user is the developer. The DataStore-backed app settings are NOT part of this migration — they live in `.preferences_pb`, a file separate from the Room database, and survive the destructive migration.

#### Scenario: First launch on a v2 build

- **WHEN** the user opens a v2 build for the first time
- **AND** a v1 database exists on disk
- **THEN** the v1 database is dropped
- **AND** a fresh v2 database is created
- **AND** the favorites, artists, and per-artist releases data is lost (acceptable for pre-release)
- **AND** the DataStore `.preferences_pb` file is preserved

#### Scenario: First launch on a device with no prior install

- **WHEN** the user opens a v2 build for the first time
- **AND** no database exists on disk
- **THEN** a fresh v2 database is created
- **AND** the `new_releases` table is initialized empty
- **AND** the DataStore `.preferences_pb` file is created on the first `setNewReleasesFloor` call (or not at all, if the user never opens the New Releases page)

### Requirement: The empty state is shown when no rows exist after a successful sync

The system MUST display an empty state when a sync completes without errors AND the `new_releases` table contains no rows (no favorite artists, or no new releases in the window). The empty state MUST explain that the user can follow artists to see their new releases.

#### Scenario: Sync completes with no results

- **WHEN** a sync completes successfully
- **AND** the `new_releases` table is empty
- **THEN** the system displays an empty-state message
- **AND** does NOT display a loading spinner

#### Scenario: Sync completes with results

- **WHEN** a sync completes successfully
- **AND** the `new_releases` table contains rows
- **THEN** the system displays the grouped list of new releases
