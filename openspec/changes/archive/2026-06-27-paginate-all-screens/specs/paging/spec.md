# Paging Capability

## Purpose

The paging capability provides a domain-agnostic library for loading, displaying, and error-handling paginated data in the FollowMyMus app. It is consumed by the search, favorites, releases, and media screens.

## ADDED Requirements

### Requirement: Paging wrapper exposes a single UI state

The paging wrapper MUST compute exactly one of four UI states from a `LazyPagingItems<T>` at any point in time: `Loading`, `Empty`, `Error`, or `Content`. The wrapper's branch methods (`OnLoading`, `OnEmpty`, `OnError`, `OnContent`) MUST be mutually exclusive: at most one runs per recomposition.

#### Scenario: Initial load with no items

- **WHEN** the paging flow has emitted and the underlying `LazyPagingItems<T>` has zero items and `loadState.refresh` is `LoadState.Loading`
- **THEN** the wrapper exposes `PagingUiState.Loading`
- **AND** only `OnLoading { ... }` runs inside the DSL body

#### Scenario: Initial load completes with zero items

- **WHEN** `loadState.refresh` is `LoadState.NotLoading` and `items.itemCount` is zero
- **THEN** the wrapper exposes `PagingUiState.Empty`
- **AND** only `OnEmpty { ... }` runs

#### Scenario: Initial load completes with items

- **WHEN** `loadState.refresh` is `LoadState.NotLoading` and `items.itemCount` is greater than zero
- **THEN** the wrapper exposes `PagingUiState.Content(items)`
- **AND** only `OnContent { items -> ... }` runs

#### Scenario: Refresh fails before any items are loaded

- **WHEN** `loadState.refresh` is `LoadState.Error` and `items.itemCount` is zero
- **THEN** the wrapper exposes `PagingUiState.Error(mappedError)`
- **AND** only `OnError { error -> ... }` runs

#### Scenario: Refresh fails after items are already loaded

- **WHEN** `loadState.refresh` is `LoadState.Error` and `items.itemCount` is greater than zero
- **THEN** the wrapper exposes `PagingUiState.Content(items)`
- **AND** the wrapper invokes the caller-provided `onErrorAction(mappedError)` exactly once per error transition
- **AND** `OnContent` runs (not `OnError`)

### Requirement: Caller provides error mapping

The wrapper MUST accept a caller-provided `errorMapper: (Throwable) -> PagingError`. The wrapper MUST NOT import any domain exception type from outside `core`. If the caller does not provide a mapper, the wrapper MUST use a default mapper that maps to `PagingError.Unknown`.

#### Scenario: Caller maps a network exception

- **WHEN** the caller passes `errorMapper = { e -> e.toSearchError() }` and the underlying Paging source throws a `SocketTimeoutException`
- **THEN** the wrapper invokes `errorMapper(SocketTimeoutException(...))`
- **AND** the resulting `PagingError` is passed to `OnError` or `onErrorAction`

#### Scenario: Default mapper is used when none is provided

- **WHEN** the caller invokes `HandlePagingItems(items)` with no `errorMapper`
- **THEN** exceptions are mapped to `PagingError.Unknown(error.message)` by the wrapper's default

### Requirement: Append errors are visible and retryable

The wrapper MUST provide an `onAppendError { error, retry -> ... }` method on its scope. This method MUST run when `loadState.append` is `LoadState.Error`. The `retry` parameter MUST be a no-arg function that, when invoked, triggers a retry of the failed append load.

#### Scenario: Append fails mid-scroll

- **WHEN** the user scrolls near the end of the list and the next page load fails
- **THEN** `onAppendError { error, retry -> ... }` runs
- **AND** the caller renders a row with `error` and a button that invokes `retry`
- **AND** invoking `retry` causes the append load to be retried

#### Scenario: Append succeeds after retry

- **WHEN** the user invokes the retry callback and the retry load succeeds
- **THEN** `onAppendError` no longer runs
- **AND** the new items are appended to the visible list

### Requirement: Append loading is rendered via a footer slot

The wrapper MUST provide `onAppendLoading { ... }` on its scope. This method MUST run when `loadState.append` is `LoadState.Loading` and `loadState.append.endOfPaginationReached` is `false`.

#### Scenario: Next page is loading

- **WHEN** the user scrolls near the end of the list and a new page is being fetched
- **THEN** `onAppendLoading { ... }` runs
- **AND** the caller typically renders a `CircularProgressIndicator` row at the list's tail

### Requirement: End-of-pagination is rendered via a footer slot

The wrapper MUST provide `onLastItem { ... }` on its scope. This method MUST run when `loadState.append.endOfPaginationReached` is `true` and `items.itemCount` is greater than zero.

#### Scenario: Final page has been loaded

- **WHEN** all pages have been loaded
- **THEN** `onLastItem { ... }` runs
- **AND** the caller typically renders an "end of list" row or leaves the slot empty

### Requirement: Paging items are rendered via a slot with stable keys

The wrapper MUST provide `onPagingItems(key = ..., contentType = ..., body = ...)` on its scope. The wrapper MUST use the provided `key` (or the position-based default if `key` is `null`) to give `LazyColumn` stable item identity. The wrapper MUST pass `contentType` to the underlying `LazyListScope.items` call.

#### Scenario: Caller provides a stable key

- **WHEN** the caller invokes `onPagingItems(key = { it.id }) { artist -> ArtistRow(artist) }`
- **THEN** each row is keyed by `artist.id`
- **AND** item recomposition is minimised when the underlying list changes

#### Scenario: Caller provides a content type for a staggered grid

- **WHEN** the caller invokes `onPagingItems(key = { it.id }, contentType = { "artist" }) { ... }`
- **THEN** the underlying `LazyListScope.items` receives `contentType = { "artist" }`
- **AND** a future staggered-grid implementation can read this

### Requirement: PagingData grouping produces header-and-item sequences

The library MUST provide a `PagingData<T>.groupedBy<K>(keySelector: (T) -> K): PagingData<GroupedItem<T, K>>` extension that inserts a `GroupedItem.Header` whenever the key changes between consecutive items, and emits `GroupedItem.Item` for every original element.

#### Scenario: Group favorites by country

- **WHEN** the caller invokes `pagingData.groupedBy<FavoriteArtist, String> { it.country }`
- **THEN** the resulting `PagingData<GroupedItem<FavoriteArtist, String>>` contains a `Header("UK")` before the first UK artist, a `Header("US")` before the first US artist, and so on
- **AND** each original artist is wrapped in `Item(artist)`

#### Scenario: Group releases by year

- **WHEN** the caller invokes `pagingData.groupedBy<Release, Int> { it.firstReleaseDate?.year ?: 0 }`
- **THEN** the resulting `PagingData<GroupedItem<Release, Int>>` contains a `Header(2023)` before the first 2023 release, a `Header(2022)` before the first 2022 release, and so on
- **AND** releases with `firstReleaseDate == null` are grouped under `Header(0)`

### Requirement: Page size defaults are centralised

The library MUST provide a `PagingDefaults` object exposing `apiConfig()` and `roomConfig()` factory functions. Repositories MUST use these factories rather than constructing `PagingConfig` literals inline.

#### Scenario: Network repository uses apiConfig

- **WHEN** a network-backed repository builds a `Pager` for an API source
- **THEN** it invokes `PagingDefaults.apiConfig()` rather than constructing `PagingConfig` directly

#### Scenario: Room repository uses roomConfig

- **WHEN** a Room-backed repository builds a `Pager` for a local source
- **THEN** it invokes `PagingDefaults.roomConfig()` rather than constructing `PagingConfig` directly

### Requirement: Total count is exposed per repository

The library MUST provide a `PagingData<T>.totalCount: Flow<Int?>` extension on repository flows (not on `PagingData` directly) so that any consumer can collect the total item count for the current query alongside the paged items. The extension MUST be implemented per-source:
- For network sources: a `StateFlow<Int?>` updated by the `PagingSource` after each successful page load.
- For Room sources: a `Flow<Int>` from a `COUNT(*)` query combined with the paging flow in the repository.

#### Scenario: Network source total count

- **WHEN** the caller collects `repository.totalArtistCount` and the search returns 1234 results
- **THEN** the latest emitted value is `1234`
- **AND** it becomes `null` again when a new search begins (before the first page returns)

#### Scenario: Room source total count

- **WHEN** the caller collects `repository.totalFavoritesCount` and the favorites table has 47 rows
- **THEN** the latest emitted value is `47`
- **AND** it updates automatically when a favorite is added or removed
