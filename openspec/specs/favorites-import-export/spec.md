# Favorites Import/Export Capability

## Purpose

The Favorites Import/Export feature lets a user move their favorites list between devices, share it with other users, or back it up as a portable file. The user triggers Export from the avatar's dropdown menu on the Favorites page, picks a destination via the platform file picker, and receives a JSON file containing the list of MusicBrainz IDs (MBIDs) currently marked as favorites. The user triggers Import from the same menu, picks a previously-exported file, and the app merges those MBIDs into the local Room database as favorites (skipping ones already present, fetching metadata for new ones from MusicBrainz in the background).

The feature is local-only on both sides: the JSON file is a portable artifact the user can put anywhere on their device, and the import operates on the local database. It does not write to the user's Supabase row — the user can press Sync afterwards to push the freshly-imported favorites to remote.

The file format is versioned (a stable `format` string and an integer `version`) so future schema changes are backwards-compatible with v1 readers via `ignoreUnknownKeys = true`. The file picker is provided by extending the existing `expect class FileHandler` in `core/system/saveQR.kt` with two new methods — `saveFile` and `openFile` — so the favorites feature reuses the same platform dispatch and per-platform plumbing that the QR-code save flow already uses. The existing `saveQR` method is NOT changed; it continues to use `Intent.ACTION_SEND` (Android) / `UIActivityViewController` (iOS) / `FileDialog.SAVE` + `Desktop.open` (JVM), because a QR image is shared with another app rather than written to a user-chosen destination. The new `saveFile` / `openFile` methods target a user-chosen destination instead and require a real file-picker UI on every platform.

## Requirements

### Requirement: Favorites export file uses a versioned JSON format

The system MUST export favorites as a JSON file with the following shape:

```json
{
  "format": "followmymus.favorites",
  "version": 1,
  "exportedAt": "2026-07-01T12:34:56Z",
  "artists": ["mbid-1", "mbid-2", "..."]
}
```

- `format` MUST be the literal string `"followmymus.favorites"`.
- `version` MUST be a positive integer (currently `1`).
- `exportedAt` MUST be an ISO-8601 string in UTC representing the export moment.
- `artists` MUST be a JSON array of MusicBrainz IDs (UUIDs) as plain strings.

The file MUST be valid JSON, encoded as UTF-8 without BOM, and MUST be parseable by `kotlinx.serialization.json.Json` configured with `ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false` — the same instance used by the Ktor client in `AppModule.provideMusicBrainzClient()`. The `format` and `version` fields are required and structurally validated on import (see "Imported file is validated for format and version"). Unknown top-level keys in the imported file MUST be silently ignored so future versions can add fields without breaking v1 readers.

The `ArtistRemoteEntity`/`SupabaseDb` surface is NOT reused for this format — the export file is a portable, user-facing artifact and MUST remain decoupled from the wire schema of the sync backend.

#### Scenario: Export writes a valid v1 file

- **WHEN** the user exports a list of favorites
- **THEN** the file at the chosen path is a valid JSON object
- **AND** the file contains `format = "followmymus.favorites"` and `version = 1`
- **AND** the `artists` array contains exactly the MBIDs that are currently favorites in the local Room database

#### Scenario: File round-trips through import

- **WHEN** the user exports favorites to a file
- **AND** then imports the same file into a fresh install
- **THEN** every MBID in the export appears in the imported favorites

#### Scenario: Unknown future fields are ignored at import

- **WHEN** a v2 file (with an additional `tags` field at the top level) is read by a v1 importer configured with `ignoreUnknownKeys = true`
- **THEN** the import proceeds successfully and the `tags` field is silently dropped

### Requirement: The existing FileHandler is extended with saveFile and openFile methods

The system MUST extend the existing `expect class FileHandler` in `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/core/system/saveQR.kt` with two new methods, leaving the existing `saveQR(image: ByteArray)` unchanged:

```kotlin
expect class FileHandler() {
    suspend fun saveQR(image: ByteArray)
    suspend fun saveFile(suggestedName: String, mimeType: String, data: ByteArray): String?
    suspend fun openFile(mimeType: String): Pair<String, ByteArray>?
}
```

- `saveFile` MUST present a platform save dialog seeded with `suggestedName` and `mimeType`, write `data` to the user's chosen destination, and return the chosen absolute file path as a `String`. It MUST return `null` if the user cancelled the picker.
- `openFile` MUST present a platform open dialog filtered to `mimeType`, read the chosen file's contents, and return a `Pair(path, bytes)` where `path` is the absolute file path and `bytes` is the file content as a UTF-8 `ByteArray`. It MUST return `null` if the user cancelled the picker.
- The methods MUST be `suspend` and honour coroutine cancellation. If the caller's scope is cancelled while the picker is open, the methods MUST return `null` rather than throwing.
- The two new methods are added to the existing `FileHandler` class — they are NOT a sibling class. This is the reuse strategy: QR-code save and favorites save/open share the same `expect/actual` dispatch, the same per-platform file plumbing, and the same Koin bean.

Each platform source set MUST add the two new `actual` methods to the existing `FileHandler` actual in the same source file as the existing `saveQR` actual:

- **Android** (`androidMain/.../core/system/saveQR.android.kt`): the `saveFile` actual MUST use `ActivityResultContracts.CreateDocument` (already used in this codebase for camera permission only — this is the first `CreateDocument` usage). The `openFile` actual MUST use `ActivityResultContracts.OpenDocument`. Both contracts MUST be registered through the current `Activity` obtained from the existing `Context` injection (`KoinComponent` + `inject<Context>()`). Content URIs returned by the contracts MUST be resolved to bytes via `contentResolver.openInputStream(uri)` (open) and `contentResolver.openOutputStream(uri)` (save). The result MUST be marshalled back to the suspend caller via `suspendCancellableCoroutine` so the picker result is `await`-able. The existing `saveQR` actual in the same file (which uses `Intent.ACTION_SEND` for share-sheet) MUST remain unchanged.
- **iOS** (`iosMain/.../core/system/saveQR.ios.kt`): the `saveFile` actual MUST use `UIDocumentPickerViewController` in `forExporting` mode. The `openFile` actual MUST use `UIDocumentPickerViewController` with `forOpeningContentTypes = [UTType.json]`, bridged through `platform.UIKit` and `platform.UniformTypeIdentifiers`. The result MUST be marshalled back via `suspendCancellableCoroutine`. The existing `saveQR` actual in the same file (which uses `UIActivityViewController` for share-sheet) MUST remain unchanged.
- **JVM** (`jvmMain/.../core/system/saveQR.kt`): the `saveFile` actual MUST use `java.awt.FileDialog` in `SAVE` mode, seeded with `suggestedName` (i.e. `fileDialog.file = suggestedName`) — this is the same API as the existing `saveQR` JVM actual, with the suggested filename parameterised. The `openFile` actual MUST use `java.awt.FileDialog` in `LOAD` mode with a `FilenameFilter` that accepts `.json` (and only `.json`) — mirroring the pattern in `screens/login/ui/parts/qrCodeScanner.jvm.kt:54-83` (which uses `FileDialog.LOAD` for image files with a `.png`/`.jpg` filter; the new actual reuses the exact same shape but with a JSON filter). The JVM `saveFile` actual MUST NOT call `Desktop.open` after writing (unlike the existing `saveQR` actual) — the user expects a quiet save, not a viewer launch.

Because `FileHandler` is already a Koin `@Single` (resolved via the existing `@ComponentScan` on `AppModule` and `DbModule`), the extended class needs no new Koin registration. The new methods are picked up automatically by the existing discovery.

#### Scenario: Android user picks a save location

- **WHEN** the user triggers an export on Android
- **THEN** the `ActivityResultContracts.CreateDocument` flow is launched with `suggestedName = "favorites-2026-07-01.json"` and `mimeType = "application/json"`
- **AND** on a positive result, the chosen content URI is opened via `contentResolver.openOutputStream(uri)` and the bytes are written
- **AND** the absolute file path is returned to the caller

#### Scenario: Android user picks an open file

- **WHEN** the user triggers an import on Android
- **THEN** the `ActivityResultContracts.OpenDocument` flow is launched with `mimeType = "application/json"`
- **AND** on a positive result, the chosen content URI is opened via `contentResolver.openInputStream(uri)` and read fully
- **AND** a `Pair(filePath, byteArray)` is returned to the caller

#### Scenario: Android user cancels the picker

- **WHEN** the user taps the back button or "Cancel" in either picker
- **THEN** the corresponding method returns `null`
- **AND** the host does NOT emit a "failed" snackbar (cancellation is not an error)

#### Scenario: iOS user picks an open file

- **WHEN** the user triggers an import on iOS
- **THEN** a `UIDocumentPickerViewController` is presented with `forOpeningContentTypes = [UTType.json]`
- **AND** on a positive result, the file's contents are read into a `ByteArray`
- **AND** a `Pair(filePath, byteArray)` is returned to the caller

#### Scenario: JVM user picks a save location

- **WHEN** the user triggers an export on the desktop build
- **THEN** a `java.awt.FileDialog` in `SAVE` mode opens, seeded with the suggested filename
- **AND** on approval, the chosen `File.getAbsolutePath()` is returned after `writeBytes(data)`
- **AND** `Desktop.getDesktop().open(file)` is NOT called (unlike `saveQR`)

#### Scenario: JVM user picks an open file

- **WHEN** the user triggers an import on the desktop build
- **THEN** a `java.awt.FileDialog` in `LOAD` mode opens with a `FilenameFilter` accepting only `.json` files
- **AND** on approval, the chosen file's contents are read via `File(path).readBytes()`
- **AND** a `Pair(filePath, byteArray)` is returned to the caller

#### Scenario: Existing saveQR behaviour is unchanged

- **WHEN** the `FileHandler.saveQR(image: ByteArray)` method is called on Android
- **THEN** the implementation continues to use `Intent.ACTION_SEND` and the share-sheet chooser
- **AND** the new `saveFile` / `openFile` methods are NOT used as a side-effect of calling `saveQR`
- **AND** the same is true for iOS (`UIActivityViewController`) and JVM (`FileDialog.SAVE` + `Desktop.open`)

#### Scenario: FileHandler is auto-discovered by Koin

- **WHEN** the Koin graph is assembled
- **THEN** the extended `FileHandler` is still a single `@Single` definition (the existing one, now with two more methods)
- **AND** `module.verify()` does NOT require any new module edits — the `@ComponentScan` on `AppModule` and `DbModule` already covers `core/system/`
- **AND** `koinInject<FileHandler>()` returns the same instance that the existing `AccountPage` already consumes

### Requirement: Avatar dropdown exposes Import and Export items

The system MUST add two `DropdownMenuItem`s to the existing `Avatar` `DropdownMenu` in the Favorites page top bar: "Export favorites" and "Import favorites". The existing "Sync" item MUST remain. The dropdown MUST contain exactly three items, in this order from top to bottom: Sync, Import favorites, Export favorites. Each new item MUST use a vector icon consistent with the existing "Sync" item (`Res.drawable.refresh_icon` family), sourced from the existing drawable resources — no new icon assets are required for v1.

The dropdown MUST close after any item is tapped. The new items MUST be disabled (non-clickable, visually dimmed) when the corresponding operation is in progress (`isImporting` or `isExporting` is `true` on `FavoritesHostState`). The avatar badge indicator (currently rendered for `isSyncing` / `hasPending`) MUST continue to reflect the sync state, not the import/export state — i.e. an active import does NOT light up the badge.

The new items MUST be passed to the `Avatar` composable as additional callback parameters — the existing `onSyncRequest: () -> Unit` is extended with `onImportRequest: () -> Unit` and `onExportRequest: () -> Unit`. The composable MUST NOT know about `FavoritesHost` or `FavoritesHostAction` directly.

#### Scenario: User opens the dropdown

- **WHEN** the user taps the avatar on the Favorites page
- **THEN** a `DropdownMenu` opens with three items in this order: Sync, Import favorites, Export favorites

#### Scenario: User taps Export

- **WHEN** the user taps "Export favorites"
- **THEN** the dropdown closes
- **AND** the host's `FavoritesHostAction.ExportRequested` is dispatched
- **AND** the host opens the platform file picker in "save" mode with a default filename like `favorites-2026-07-01.json`

#### Scenario: User taps Import

- **WHEN** the user taps "Import favorites"
- **THEN** the dropdown closes
- **AND** the host's `FavoritesHostAction.ImportRequested` is dispatched
- **AND** the host opens the platform file picker in "open" mode filtered to JSON files

#### Scenario: Dropdown item is disabled during operation

- **WHEN** `state.isImporting == true`
- **THEN** the "Import favorites" item is visually disabled and does not respond to taps
- **AND** "Export favorites" and "Sync" remain enabled
- **AND** the same logic applies symmetrically: when `state.isExporting == true`, "Export favorites" is disabled and the other two remain enabled

#### Scenario: Avatar badge is unaffected by import/export

- **WHEN** an import is in progress
- **THEN** the avatar's badge (currently shown for `isSyncing` / `hasPending`) is rendered the same as before the import started
- **AND** the shimmer animation is NOT triggered by import activity

### Requirement: A FavoritesImportExportRepository is registered as a Koin single

The system MUST add a `FavoritesImportExportRepository` class in `composeApp/src/commonMain/.../musicBrainz/domain/FavoritesImportExportRepository.kt`, registered as a Koin `@Single(binds = [FavoritesImportExportRepository::class])`. The class MUST depend on the existing `ArtistDao`, `FavoriteDao`, and `SearchEngine` (all already in the Koin graph). The class MUST NOT depend on `SupabaseDb` — import/export is local-only. The class MUST NOT depend on `FileHandler` — file I/O is the host's concern, not the repository's. The repository works in bytes, not paths.

The repository MUST expose two suspend methods that return the project's standard `Result<T>` (defined in `core/resultApi.kt` — `sealed class Result<out T> { data class Success<out R>(value: R) : Result<R>(); data class Error(type: ErrorType, message: String? = null) : Result<Nothing>() }`). The methods MUST NOT throw on application-level errors — instead, they MUST wrap any failure in `Result.Error(FavoritesImportExportError.Xxx)`. The host unwraps the `Result` via the existing `forSuccess { ... }` / `forError { type, message -> ... }` extension functions in `core/resultApi.kt`.

The typed error hierarchy is a new `sealed class FavoritesImportExportError : ErrorType()` (matching the existing `SupabaseError` / `SearchError` / `SessionError` / `BrainzApiError` pattern in `core/`). The cases are:

```kotlin
sealed class FavoritesImportExportError : ErrorType() {
    // export-side
    data class DataReadError(val cause: String?) : FavoritesImportExportError()

    // import-side (fatal — the file cannot be processed at all)
    data object Malformed : FavoritesImportExportError()          // JSON parse error or wrong shape
    data object UnsupportedFormat : FavoritesImportExportError()  // `format` field is wrong
    data class UnsupportedVersion(val version: Int) : FavoritesImportExportError()
    data object MissingArtistsField : FavoritesImportExportError() // `artists` missing or not an array
    data class EmptyArtistEntry(val index: Int) : FavoritesImportExportError() // blank string in `artists`
    data class NetworkError(val cause: String?) : FavoritesImportExportError() // all new MBIDs failed to fetch (likely no network)
}
```

Per-MBID failures (one artist 404s, another times out) are NOT typed errors — they are partial outcomes reported in the success payload's `failed` count. A file with five MBIDs where two of them 404 is still a `Result.Success(ImportSummary(imported = 3, skipped = 0, failed = 2))`, not a `Result.Error`. This split matches the existing `ApiSyncRepository.syncRemote()` pattern, which accumulates per-row errors in an `ErrorType` list rather than failing the whole call.

The `NetworkError` case is reserved for the special situation where **every** new MBID failed to fetch (i.e. `imported == 0 && skipped == 0 && failed > 0` after the merge step). In that scenario the import returns `Result.Error(NetworkError(cause))` instead of `Result.Success(ImportSummary(0, 0, N))`, so the host can show a "check your connection" snackbar rather than a misleading "Imported 0 favorites, skipped 0 — N failed to fetch" success message. The `cause` is the first error message encountered (network timeout, 5xx, etc.).

The repository MUST use the same `kotlinx.serialization.json.Json` instance as the Ktor client (i.e. the `Json` bean from `AppModule.provideMusicBrainzClient()` configuration) for parsing the imported file. For serialisation on export, the repository MAY use its own local `Json` instance with the same configuration, since it does not need HTTP-aware features.

The repository MUST perform all MusicBrainz fetches on `Dispatchers.IO` (or via a coroutine launched with that context). The repository MUST NOT perform file I/O at all — no `File()`, no `readBytes`, no `writeBytes`. The host reads the file (via `FileHandler.openFile`) and writes the file (via `FileHandler.saveFile`); the repository just serialises and parses bytes.

The repository MUST honor coroutine cancellation: if the caller's scope is cancelled mid-import, the in-flight `flatMapMerge` chunk coroutines are cancelled and partial state is acceptable (already-inserted rows remain in the DB — the import is additive). The cancelled import does NOT emit a snackbar (the host's scope is gone, so the snackbar channel is unreachable).

#### Scenario: Repository resolves through Koin

- **WHEN** `FavoritesHost` is instantiated
- **THEN** `koinInject<FavoritesImportExportRepository>()` returns a single instance
- **AND** `module.verify()` recognizes `FavoritesImportExportRepository` as a registered `single` definition bound to its interface

#### Scenario: Repository depends on existing beans, not on FileHandler

- **WHEN** the Koin graph is assembled
- **THEN** `FavoritesImportExportRepository` is constructed with `ArtistDao`, `FavoriteDao`, and `SearchEngine` resolved from the existing Koin bindings
- **AND** `FavoritesImportExportRepository` is NOT injected with `FileHandler` (file I/O is the host's responsibility)
- **AND** no other dependency is required

#### Scenario: Repository returns Result, not raw types

- **WHEN** the host calls any method on `FavoritesImportExportRepository`
- **THEN** the return type is `Result<T>` (where `T` is the success payload — see the export and import requirements below)
- **AND** application-level errors are returned as `Result.Error(FavoritesImportExportError.Xxx)`, NOT thrown
- **AND** the host unwraps via the existing `forSuccess { ... }` / `forError { type, message -> ... }` extensions in `core/resultApi.kt`

### Requirement: Export serialises the current favorites to a byte payload, wrapped in Result

The system MUST provide a `suspend fun serializeExport(): Result<FavoritesExportPayload>` method on `FavoritesImportExportRepository`. Where `FavoritesExportPayload` is a `data class(bytes: ByteArray, count: Int)` carrying both the encoded JSON bytes AND the count of MBIDs in the payload (the host needs the count for the success snackbar; including it in the payload avoids a second DB round-trip). The method MUST:

1. Read the current list of favorite MBIDs from the local Room database — i.e. the `id` column of every `ArtistEntity` row with `isFavorite = true`, queried once via `FavoriteDao.getFavoriteArtistsIds().first()` (or an equivalent one-shot query, not a `Flow` subscription).
2. Sort the MBIDs lexicographically (case-insensitive) so the exported payload is stable across runs and diff-friendly.
3. Serialise the list to a `FavoritesExportFile` data class with the shape described in "Favorites export file uses a versioned JSON format" (`format`, `version`, `exportedAt` set to `Clock.System.now().toString()`, `artists`).
4. Encode the result to a UTF-8 JSON `ByteArray` and return `Result.Success(FavoritesExportPayload(bytes = ..., count = artists.size))`. The method does NOT touch the filesystem — the bytes are the full payload the host will hand to `FileHandler.saveFile` for writing.
5. On any DB read failure (e.g. Room throws `SQLiteException`), return `Result.Error(FavoritesImportExportError.DataReadError(cause = e.message))`. The method MUST NOT throw to the caller.

#### Scenario: Export with 10 favorites

- **WHEN** the user has 10 local favorites
- **AND** the host calls `repository.serializeExport()`
- **THEN** the result is `Result.Success(FavoritesExportPayload(bytes = <UTF-8 JSON>, count = 10))`
- **AND** the `artists` array in the JSON is sorted lexicographically

#### Scenario: Export with 0 favorites

- **WHEN** the user has no local favorites
- **THEN** the result is `Result.Success(FavoritesExportPayload(bytes = <UTF-8 JSON with empty artists array>, count = 0))`
- **AND** `format`, `version`, `exportedAt` are still populated in the JSON

#### Scenario: Host writes the bytes via FileHandler.saveFile

- **WHEN** the host receives `Result.Success(payload)` from `serializeExport()`
- **THEN** the host passes `payload.bytes` to `fileHandler.saveFile("favorites-2026-07-01.json", "application/json", payload.bytes)`
- **AND** the host uses `payload.count` for the success snackbar
- **AND** `FileHandler.saveFile` is the only thing that writes to the user's chosen destination
- **AND** the repository never sees the destination path

#### Scenario: Export to a non-writable destination

- **WHEN** the user picks a destination that is not writeable (e.g. read-only filesystem)
- **THEN** `fileHandler.saveFile` returns `null` (the platform-specific contract)
- **AND** the host emits a "Failed to export: cannot write file" snackbar
- **AND** the repository is not involved in the failure path (it already returned the bytes successfully)

#### Scenario: Export with a corrupted DB

- **WHEN** the local DB is in a corrupted state (e.g. Room throws `SQLiteException`)
- **THEN** the result is `Result.Error(FavoritesImportExportError.DataReadError(cause = "..."))`
- **AND** the host emits a "Failed to export: cannot read favorites" snackbar via `forError { type, _ -> ... }`
- **AND** the host does NOT call `fileHandler.saveFile` (no destination is shown)
- **AND** the exception does NOT propagate to the scope's uncaught-exception handler

### Requirement: Import parses a byte payload and merges favorites into the local DB, returning Result

The system MUST provide a `suspend fun importFromBytes(bytes: ByteArray, sourceName: String? = null): Result<ImportSummary>` method on `FavoritesImportExportRepository`. Where `ImportSummary` is a `data class(imported: Int, skipped: Int, failed: Int)` (all non-negative). The `sourceName` parameter is optional and used only for error messages (e.g. `"Failed to parse /path/to/file.json"`) — the repository does NOT use it to read the file (the bytes are passed directly). The method MUST:

1. Parse the bytes as a UTF-8 string and feed it to the project `Json` (configured with `ignoreUnknownKeys = true`).
2. Validate `format == "followmymus.favorites"` and `version == 1`. On validation failure, return `Result.Error(FavoritesImportExportError.UnsupportedFormat)`, `Result.Error(FavoritesImportExportError.UnsupportedVersion(actualVersion))`, `Result.Error(FavoritesImportExportError.Malformed)`, or `Result.Error(FavoritesImportExportError.MissingArtistsField)` / `Result.Error(FavoritesImportExportError.EmptyArtistEntry(index))` as appropriate (see "Imported file is validated for format and version" for the exact rules). The `sourceName` (if provided) is included in the `ErrorType.message` for context. The method MUST NOT throw.
3. Deduplicate the input `artists` list (set semantics — same MBID appearing multiple times counts as one).
4. Partition the unique MBIDs into "already favorite" (via `FavoriteDao.getFavoriteArtistsIds().first()`) and "new" (the rest). Count the "already favorite" MBIDs into `skipped` immediately — they are not re-fetched.
5. Chunk the "new" MBIDs into batches of **100** IDs each. The last chunk MAY be smaller. This matches the existing batched-MusicBrainz-query pattern (the same 100-item limit is used by `ApiReleasesRepository.syncReleases` for `arid:(...)` queries). For each chunk, schedule a batched fetch via `SearchEngine` — concurrent launch is `flatMapMerge(concurrency = 50)`, matching `PagingDefaults.API_PAGE`. The `RateLimitedApiQueue` (1 req/sec) serialises the actual HTTP calls; the 50-concurrent cap is for coroutine management, not for rate-limiting.
6. Each batched fetch is a `GET /ws/2/artist?query=arid:(id1 OR id2 OR ... OR idN)&limit=100&fmt=json` request, mirroring the existing release-group batched fetch in `ApiReleasesRepository.syncReleases`. The `SearchEngine` MUST expose a `searchArtistsByIdBatch(ids: List<String>): List<ArtistDto>` method (or equivalent) that wraps this batched query — the implementation is a new method on `ApiSearchEngine` and is shared with the per-artist `getArtistById` path.
7. For each artist returned by the batched response, call `artistsRepository.addToFavorite(artist.id)`. This is the SAME `addToFavorite` method that the search page uses. As a side-effect of the favorites-import-export feature, `addToFavorite` is extended in this change with a network-fallback path: if `searchEngine.getArtistFromCache(artistId)` returns null (the artist is not in the local cache), the method now calls `searchEngine.getArtistById(artistId)` (network fetch) before falling back to no-op. This benefits both the import flow AND the existing search-page "star" action, which previously failed silently when the artist was not cached.
8. Each `addToFavorite(id)` call uses `dbRepository.insertArtist(artist) { toEntity(true, SyncStatus.PendingRemoteAdd) }`. The existing `insertArtist` call uses Room's `OnConflictStrategy.REPLACE`, so an existing `ArtistEntity` row with `isFavorite = false` is rewritten to `isFavorite = true` and `syncStatus = PendingRemoteAdd` — the "flip from non-favorite" path is handled transparently by the upsert.
9. Count outcomes: `imported` is incremented for each `addToFavorite` that successfully inserts a favorite (whether the row was new or flipped from non-favorite). `failed` is incremented for each chunk-MBID that was queried but did NOT appear in the batched response (deleted artist / 404) AND for each chunk where the batched query itself returned a transient error (5xx, timeout). The first such transient error's message is captured as the `cause` for a potential `Result.Error(NetworkError(...))` (see step 10).
10. After all chunks are processed, apply the "all-failed" rule:
    - If `imported > 0` OR `skipped > 0` (i.e. at least one new MBID was imported, OR at least one MBID was already a favorite), return `Result.Success(ImportSummary(imported, skipped, failed))`. Partial success is a success.
    - If `imported == 0 && skipped == 0 && failed > 0` (i.e. every new MBID failed to fetch — most likely cause is no network), return `Result.Error(FavoritesImportExportError.NetworkError(cause = <first error message>))`. This lets the host show a "check your connection" snackbar rather than a misleading "Imported 0, skipped 0 — N failed" success message.
11. The invariant `imported + skipped + failed == artists.size` (counted after dedup) MUST hold when the result is `Result.Success`. The `Result.Error` case is reserved for validation failures AND the "all-failed network" case — partial success is always `Result.Success`.

The import MUST honor coroutine cancellation: if the host's scope is cancelled, the in-flight `flatMapMerge` coroutines are cancelled. Already-inserted rows remain in the DB (the import is additive — partial state is acceptable).

#### Scenario: Import a fresh file with 5 new MBIDs

- **WHEN** the user imports a JSON file with 5 MBIDs
- **AND** the local DB has no matching artists
- **THEN** the 5 MBIDs form a single chunk (5 ≤ 100)
- **AND** one batched MusicBrainz request is issued
- **AND** the response contains all 5 artists
- **AND** `addToFavorite` is called for each, inserting them as favorites with `syncStatus = PendingRemoteAdd`
- **AND** the method returns `Result.Success(ImportSummary(imported = 5, skipped = 0, failed = 0))`

#### Scenario: Import a file with 3 already-present MBIDs

- **WHEN** the user imports a JSON file with 3 MBIDs
- **AND** all 3 are already in the local DB as favorites (`isFavorite = true`)
- **THEN** the 3 are counted as `skipped` immediately
- **AND** no batched MusicBrainz request is issued
- **AND** the method returns `Result.Success(ImportSummary(imported = 0, skipped = 3, failed = 0))`

#### Scenario: Import a mix of new and already-present MBIDs

- **WHEN** the user imports a JSON file with 5 MBIDs
- **AND** 2 are already in the local DB as favorites
- **THEN** the 2 are counted as `skipped`
- **AND** the 3 new ones form one chunk
- **AND** the 3 new ones are fetched and inserted as favorites
- **AND** the method returns `Result.Success(ImportSummary(imported = 3, skipped = 2, failed = 0))`

#### Scenario: Some MBIDs fail to fetch (deleted artists)

- **WHEN** the user imports a JSON file with 5 MBIDs
- **AND** 2 of them are deleted from MusicBrainz (the batched response contains 3 artists)
- **THEN** the 3 returned artists are inserted as favorites
- **AND** the 2 missing MBIDs are recorded in `failed`
- **AND** the method returns `Result.Success(ImportSummary(imported = 3, skipped = 0, failed = 2))` — NOT `Result.Error` (partial success is a success)
- **AND** the host shows the failed count in the success snackbar

#### Scenario: All new MBIDs fail to fetch (network outage)

- **WHEN** the user imports a JSON file with 5 new MBIDs
- **AND** the user has no network connection (the batched request times out)
- **THEN** the 5 MBIDs form a single chunk
- **AND** the batched request fails with a timeout / connection error
- **AND** no rows are inserted
- **AND** the method returns `Result.Error(FavoritesImportExportError.NetworkError(cause = "timeout" or similar))`
- **AND** the host shows a "Failed to import: no network connection, please try again" snackbar
- **AND** the app does NOT crash

#### Scenario: Import is additive, not destructive

- **WHEN** the user imports a JSON file
- **THEN** the existing local favorites are NOT removed
- **AND** no `ArtistEntity` row is deleted as part of the import
- **AND** the local favorites count after import equals `pre_count + imported` (failures do not affect existing rows)

#### Scenario: Importing twice is idempotent

- **WHEN** the user imports a JSON file with 5 MBIDs
- **AND** then imports the same file again
- **THEN** the second import returns `Result.Success(ImportSummary(imported = 0, skipped = 5, failed = 0))`
- **AND** no duplicate rows are created in `ArtistEntity`
- **AND** no duplicate `supabase` upserts are enqueued

#### Scenario: Duplicate MBIDs in the import file are deduplicated

- **WHEN** the user imports a JSON file where the same MBID appears 3 times
- **THEN** that MBID is processed only once
- **AND** the returned counts sum to 1 for that file, not 3

#### Scenario: Concurrent imports do not produce duplicate rows

- **WHEN** two imports of the same file are started concurrently (e.g. the user double-taps)
- **THEN** the `ArtistEntity` primary key constraint (`id`) prevents duplicate rows
- **AND** the second concurrent import sees the first's inserts and reports them as `skipped`

#### Scenario: Existing non-favorite artist is flipped to favorite

- **WHEN** the user imports an MBID
- **AND** the local `ArtistEntity` row exists with `isFavorite = false` (e.g. from a previous search that did not star the artist)
- **THEN** `addToFavorite` is called and the upsert rewrites the row to `isFavorite = true` and `syncStatus = PendingRemoteAdd`
- **AND** the artist is counted as `imported` (not `skipped`)
- **AND** the search page's row for this artist now shows the favorited state on next render

#### Scenario: Large import with multiple chunks

- **WHEN** the user imports a JSON file with 250 new MBIDs
- **THEN** the 250 MBIDs are chunked into 3 batches of 100, 100, 50
- **AND** the 3 chunk-coroutines are launched concurrently via `flatMapMerge(concurrency = 50)`
- **AND** `RateLimitedApiQueue` serialises the 3 actual HTTP calls at 1 req/sec
- **AND** total wall-clock time is roughly `3 sec + 3 * avg_request_time` (typically 6-12 sec)
- **AND** the method returns `Result.Success(ImportSummary(imported, skipped, failed))` (or `Result.Error(NetworkError)` if all chunks fail)

#### Scenario: sourceName is included in error messages

- **WHEN** the user picks a file at `/some/path/old-favorites.json`
- **AND** the file has `version = 2`
- **THEN** the result is `Result.Error(FavoritesImportExportError.UnsupportedVersion(version = 2))`
- **AND** the `ErrorType.message` (if the host reads it via `forError { type, message -> ... }`) includes the string `/some/path/old-favorites.json` for context
- **AND** the host surfaces this in the "Failed to import" snackbar

#### Scenario: sourceName is optional

- **WHEN** the host calls `importFromBytes(bytes)` without a `sourceName`
- **THEN** the method still works
- **AND** any `Result.Error` messages omit the source name (e.g. just `"Unsupported file version"` without a path)

#### Scenario: Empty artists array is not an error

- **WHEN** the user imports a file with `artists: []` and a valid `format` and `version`
- **THEN** the result is `Result.Success(ImportSummary(imported = 0, skipped = 0, failed = 0))`
- **AND** the host shows a "Imported 0 favorites" snackbar (or similar "nothing to import" message — exact wording is a UI detail)

### Requirement: Imported file is validated for format and version, returning Result.Error

The system MUST validate the imported JSON file structurally before processing its contents. Validation MUST fail (returning `Result.Error(FavoritesImportExportError.Xxx)`) if:

- The file cannot be parsed as JSON (malformed, wrong encoding, empty) → `Result.Error(Malformed)`.
- The top-level object is not a JSON object (e.g. a JSON array, a primitive) → `Result.Error(Malformed)`.
- The `format` field is missing, not a string, or not equal to `"followmymus.favorites"` → `Result.Error(UnsupportedFormat)`.
- The `version` field is missing, not an integer, or not equal to `1` (the current version) → `Result.Error(UnsupportedVersion(actualVersion))`.
- The `artists` field is missing or is not a JSON array of strings → `Result.Error(MissingArtistsField)`.
- Any string in the `artists` array is empty → `Result.Error(EmptyArtistEntry(index))`.

The system MUST NOT validate the MBID against MusicBrainz during validation — that fetch happens during the merge step. Validation is purely structural and runs without any network call.

The host MUST route `Result.Error(FavoritesImportExportError.Xxx)` from `importFromBytes` to a snackbar (see "Import and export surface success and error snackbars"). The host MUST NOT crash on a malformed file; the `Result.Error` path is the application's normal error-handling flow, not an exception.

#### Scenario: User imports a non-JSON file

- **WHEN** the user picks a `.txt` file by mistake
- **THEN** the result is `Result.Error(FavoritesImportExportError.Malformed)`
- **AND** the host routes this to a "Failed to import: file is not valid JSON" snackbar via `forError { type, _ -> ... }`

#### Scenario: User imports a JSON file with the wrong format

- **WHEN** the user picks a JSON file that has `format = "other.app.favorites"`
- **THEN** the result is `Result.Error(FavoritesImportExportError.UnsupportedFormat)`
- **AND** the host emits an "Unsupported file format" snackbar

#### Scenario: User imports a JSON file with a higher version

- **WHEN** the user picks a file exported by a future app version (`version = 2`)
- **THEN** the result is `Result.Error(FavoritesImportExportError.UnsupportedVersion(version = 2))`
- **AND** the host emits an "Unsupported file version (got 2, expected 1)" snackbar

#### Scenario: User imports a JSON file with a missing artists field

- **WHEN** the user picks a JSON file that has `format` and `version` but no `artists` key
- **THEN** the result is `Result.Error(FavoritesImportExportError.MissingArtistsField)`
- **AND** the host emits a "Failed to import: missing artists field" snackbar

#### Scenario: User imports a valid file

- **WHEN** the user picks a valid v1 file
- **THEN** the result is `Result.Success(ImportSummary(...))`
- **AND** no validation snackbar is emitted
- **AND** the host shows the success snackbar with the import summary

### Requirement: New actions are added to the FavoritesHostAction sealed interface

The system MUST add two new `data object` cases to `FavoritesHostAction`:

```kotlin
data object ImportRequested : FavoritesHostAction
data object ExportRequested : FavoritesHostAction
```

The `invoke(action: FavoritesHostAction)` method on `FavoritesHost` MUST handle these by:

- **`ExportRequested`**: set `isExporting = true` on the state, then call `repository.serializeExport()` which returns `Result<FavoritesExportPayload>`. The host MUST unwrap the `Result` via the existing `forSuccess { payload -> ... }` / `forError { type, message -> ... }` extensions from `core/resultApi.kt`:
  - On `forSuccess { payload -> ... }`: call `fileHandler.saveFile(suggestedName = "favorites-${today}.json", mimeType = "application/json", data = payload.bytes)`. If the picker returns a non-null path, emit `SnackbarMsg(key = "favorites.export.success", message = "Exported ${payload.count} favorites")` (mapped to `Res.string.favorites_export_success` with a `%d` argument). If the picker returns null, do not emit any snackbar (cancellation).
  - On `forError { type, _ -> ... }`: emit `SnackbarMsg(key = "favorites.export.error", message = "Failed to export: cannot read favorites")` (mapped to `Res.string.favorites_export_error_data_read`). Do NOT call `fileHandler.saveFile` on the error path.
  - In a `finally` block, set `isExporting = false`.
- **`ImportRequested`**: set `isImporting = true` on the state, then call `fileHandler.openFile(mimeType = "application/json")`. If the picker returns null, do not emit any snackbar (cancellation) and return. Otherwise, call `repository.importFromBytes(bytes, sourceName = path)` which returns `Result<ImportSummary>`. The host MUST unwrap the `Result` via the same `forSuccess` / `forError` extensions:
  - On `forSuccess { summary -> ... }`: emit `SnackbarMsg(key = "favorites.import.success", message = <summary-driven>)`. The message format depends on `summary.failed`:
    - `failed == 0`: `"Imported ${summary.imported} favorites, skipped ${summary.skipped} already present"` (`Res.string.favorites_import_success_clean` with `%1$d, %2$d` args).
    - `failed > 0`: `"Imported ${summary.imported} favorites, skipped ${summary.skipped} already present — ${summary.failed} failed to fetch"` (`Res.string.favorites_import_success_with_failures` with `%1$d, %2$d, %3$d` args).
  - On `forError { type, message -> ... }`: emit `SnackbarMsg(key = "favorites.import.error", message = <type-driven>)`. The message is derived from the typed error:
    - `Malformed` → `"Failed to import: file is not valid JSON"` (`Res.string.favorites_import_error_malformed`).
    - `UnsupportedFormat` → `"Failed to import: Unsupported file format"` (`Res.string.favorites_import_error_unsupported_format`).
    - `UnsupportedVersion(version = N)` → `"Failed to import: Unsupported file version (got N, expected 1)"` (`Res.string.favorites_import_error_unsupported_version` with `%d` arg).
    - `MissingArtistsField` → `"Failed to import: missing artists field"` (`Res.string.favorites_import_error_missing_artists`).
    - `EmptyArtistEntry(index = I)` → `"Failed to import: empty artist entry at index I"` (`Res.string.favorites_import_error_empty_artist_entry` with `%d` arg).
    - `NetworkError(cause)` → `"Failed to import: no network connection, please try again"` (`Res.string.favorites_import_error_network`).
  - In a `finally` block, set `isImporting = false`.

Both flows MUST run in the host's `scope` (the `coroutineScope()` obtained from `componentContext`). Both flows MUST be exception-safe — the only way an error reaches the host is via `Result.Error`, NOT via a thrown exception. If an unexpected exception does escape (e.g. an unanticipated `IllegalStateException` from Room), the host MUST catch it in a top-level `try { ... } catch (e: Throwable) { ... }` and route it to a generic-failure snackbar (`Res.string.favorites_import_error_unknown`) rather than letting it crash the app via the scope's uncaught-exception handler. This catch-all is for safety; the typed `Result.Error` path is the normal error-handling flow.

The `FavoritesHost` constructor MUST accept a new `FileHandler` dependency (in addition to the existing `syncRepository`). The dependency is resolved from Koin via the class-level `@Factory` annotation on `FavoritesHost` — no new `@Module` provider is required because `FileHandler` is already discovered by `@ComponentScan` on `AppModule` and `DbModule`. The constructor MUST also accept the new `FavoritesImportExportRepository` dependency.

#### Scenario: User triggers ExportRequested

- **WHEN** the user taps "Export favorites" in the dropdown
- **THEN** the host receives `FavoritesHostAction.ExportRequested`
- **AND** the host sets `isExporting = true`
- **AND** the host calls `repository.serializeExport()` which returns `Result<FavoritesExportPayload>`
- **AND** on `forSuccess`, the host calls `fileHandler.saveFile("favorites-2026-07-01.json", "application/json", payload.bytes)`

#### Scenario: User picks a file and the export completes

- **WHEN** the user picks a save location at `/some/path/favorites-2026-07-01.json`
- **AND** the file is written successfully
- **THEN** the host emits `SnackbarMsg(key = "favorites.export.success", message = getString(Res.string.favorites_export_success, payload.count))` — i.e. `"Exported 12 favorites"` for `count = 12`
- **AND** the host sets `isExporting = false` in a `finally` block

#### Scenario: User cancels the file picker

- **WHEN** the file picker returns `null` (user cancelled)
- **THEN** the host resets `isExporting` / `isImporting` to `false`
- **AND** no snackbar is emitted
- **AND** no file is written

#### Scenario: User triggers ImportRequested and a file is picked

- **WHEN** the user picks a file at `/some/path/old-favorites.json`
- **AND** the file is a valid v1 export
- **THEN** the host sets `isImporting = true`
- **AND** the host calls `fileHandler.openFile("application/json")`
- **AND** the host receives a non-null `Pair("/some/path/old-favorites.json", bytes)` from the picker
- **AND** the host calls `repository.importFromBytes(bytes, sourceName = "/some/path/old-favorites.json")` which returns `Result<ImportSummary>`
- **AND** on `forSuccess { summary -> ... }`, the host emits a snackbar with `summary.imported` / `summary.skipped` / `summary.failed`

#### Scenario: Repository returns Result.Error on validation failure

- **WHEN** the picked file has `format = "wrong.format"`
- **AND** `repository.importFromBytes(...)` returns `Result.Error(FavoritesImportExportError.UnsupportedFormat)`
- **THEN** the host unwraps via `forError { type, _ -> ... }` in the `invoke` handler
- **AND** the host emits `SnackbarMsg(key = "favorites.import.error", message = getString(Res.string.favorites_import_error_unsupported_format))` — i.e. `"Failed to import: Unsupported file format"`
- **AND** the host sets `isImporting = false` in the `finally` block
- **AND** the app does NOT crash (no thrown exception, normal control flow)

#### Scenario: Repository returns Result.Error on DB read failure

- **WHEN** the user taps "Export favorites"
- **AND** the local DB is in a corrupted state (e.g. Room throws `SQLiteException`)
- **AND** `repository.serializeExport()` returns `Result.Error(FavoritesImportExportError.DataReadError(cause = "..."))`
- **THEN** the host unwraps via `forError { type, _ -> ... }`
- **AND** the host emits `SnackbarMsg(key = "favorites.export.error", message = getString(Res.string.favorites_export_error_data_read))` — i.e. `"Failed to export: cannot read favorites"`
- **AND** the host does NOT call `fileHandler.saveFile` (no destination is shown)
- **AND** the host sets `isExporting = false` in the `finally` block
- **AND** the `SQLiteException` does NOT propagate to the scope's uncaught-exception handler (the repository caught it and returned `Result.Error`)

#### Scenario: Repository returns Result.Error on network outage

- **WHEN** the user picks a file with 10 MBIDs
- **AND** the user has no network connection
- **AND** every chunk's batched MusicBrainz request fails with a timeout
- **AND** `repository.importFromBytes(...)` returns `Result.Error(FavoritesImportExportError.NetworkError(cause = "timeout"))`
- **THEN** the host unwraps via `forError { type, _ -> ... }`
- **AND** the host emits `SnackbarMsg(key = "favorites.import.error", message = getString(Res.string.favorites_import_error_network))` — i.e. `"Failed to import: no network connection, please try again"`
- **AND** the host sets `isImporting = false` in the `finally` block
- **AND** no rows were inserted in the DB

#### Scenario: Host handles per-MBID partial failure via success payload

- **WHEN** the picked file has 10 MBIDs
- **AND** 7 are imported, 2 are skipped (already favorites), 1 fails to fetch
- **AND** `repository.importFromBytes(...)` returns `Result.Success(ImportSummary(imported = 7, skipped = 2, failed = 1))`
- **THEN** the host unwraps via `forSuccess { summary -> ... }`
- **AND** the host emits `SnackbarMsg(key = "favorites.import.success", message = getString(Res.string.favorites_import_success_with_failures, 7, 2, 1))` — i.e. `"Imported 7 favorites, skipped 2 already present — 1 failed to fetch"`
- **AND** the result is `Result.Success` (NOT `Result.Error`) — partial success is the success path

### Requirement: FavoritesHostState gains importing and exporting flags

The system MUST extend `FavoritesHostState` (a `@Serializable data class`) with two new boolean fields:

```kotlin
@Serializable
data class FavoritesHostState(
    val artistIdSelected: String? = null,
    val releaseIdSelected: String? = null,
    val backVisible: Boolean = false,
    val avatar: AvatarState = AvatarState(),
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
)
```

The two new fields MUST have default values (`false`) so existing saved states (e.g. from a previous build) remain valid. The two new fields MUST survive config changes (rotation, theme switch) because `FavoritesHostState` is held by `saveableMutableValue`. They MUST NOT be relied on to survive process death (acceptable for transient operation flags — a process kill mid-import means a fresh state on relaunch, with the import partially applied to the DB).

#### Scenario: State is updated when import starts

- **WHEN** the host begins an import (the picker has returned a non-null path)
- **THEN** `_state.update { it.copy(isImporting = true) }` is called
- **AND** the avatar dropdown's "Import favorites" item is visually disabled

#### Scenario: State is reset in a finally block

- **WHEN** an import completes (success, validation error, or I/O error)
- **THEN** `_state.update { it.copy(isImporting = false) }` is called in a `finally` block
- **AND** the dropdown item is re-enabled

#### Scenario: Existing serialized state still loads

- **WHEN** a user updates the app from a build that did not have `isImporting`/`isExporting`
- **AND** the existing `FavoritesHostState` JSON is restored from `saveableMutableValue`
- **THEN** the deserialised state has `isImporting = false` and `isExporting = false` (the default values are applied to the missing fields)

### Requirement: Import and export surface success and error snackbars via Res.string

The system MUST emit a `SnackbarMsg` through the existing `FavoritesHost.events: Channel<SnackbarMsg>` on each operation, using a stable, distinct `key` per operation kind. All snackbar messages MUST be sourced from Compose Multiplatform resources (the existing `composeApp/src/commonMain/composeResources/values/strings.xml` file, in the project's standard `Res.string` namespace) — no hardcoded English strings in the host. New resource entries MUST be added in the same change:

- `Res.string.favorites_export_success` — `"Exported %d favorites"` (1 arg: count)
- `Res.string.favorites_export_error_data_read` — `"Failed to export: cannot read favorites"`
- `Res.string.favorites_import_success_clean` — `"Imported %1$d favorites, skipped %2$d already present"` (2 args: imported, skipped)
- `Res.string.favorites_import_success_with_failures` — `"Imported %1$d favorites, skipped %2$d already present — %3$d failed to fetch"` (3 args: imported, skipped, failed)
- `Res.string.favorites_import_error_malformed` — `"Failed to import: file is not valid JSON"`
- `Res.string.favorites_import_error_unsupported_format` — `"Failed to import: Unsupported file format"`
- `Res.string.favorites_import_error_unsupported_version` — `"Failed to import: Unsupported file version (got %d, expected 1)"`
- `Res.string.favorites_import_error_missing_artists` — `"Failed to import: missing artists field"`
- `Res.string.favorites_import_error_empty_artist_entry` — `"Failed to import: empty artist entry at index %d"`
- `Res.string.favorites_import_error_network` — `"Failed to import: no network connection, please try again"`
- `Res.string.favorites_import_error_unknown` — `"Failed to import: unexpected error"` (catch-all)

The `key` field MUST be one of four stable keys, derived from the operation kind:
- `"favorites.export.success"` — for `Res.string.favorites_export_success`
- `"favorites.export.error"` — for `Res.string.favorites_export_error_data_read`
- `"favorites.import.success"` — for both success variants (`favorites_import_success_clean` and `favorites_import_success_with_failures`)
- `"favorites.import.error"` — for all error variants (validation, network, unknown)

The stable keys prevent the existing `distinctUntilChanged` operator on the events flow from deduping legitimate consecutive operations (the current "Sync" flow uses a single key, which would cause the second import to be silently dropped if the keys were all the same).

The snackbar message MUST be distinct from any in-flight sync snackbar (different key, different prefix). The host MUST continue to render snackbars through the existing `ScaffoldSlots.snackbarMessages` flow.

#### Scenario: Successful export

- **WHEN** `repository.serializeExport()` returns `Result.Success(FavoritesExportPayload(bytes = ..., count = 12))`
- **AND** the user picks a save location
- **THEN** a snackbar appears with `key = "favorites.export.success"` and `message = getString(Res.string.favorites_export_success, 12)` — i.e. `"Exported 12 favorites"`

#### Scenario: Successful import with no duplicates

- **WHEN** `repository.importFromBytes(...)` returns `Result.Success(ImportSummary(imported = 5, skipped = 0, failed = 0))`
- **THEN** a snackbar appears with `key = "favorites.import.success"` and `message = getString(Res.string.favorites_import_success_clean, 5, 0)` — i.e. `"Imported 5 favorites, skipped 0 already present"`

#### Scenario: Import with duplicates and failures

- **WHEN** `repository.importFromBytes(...)` returns `Result.Success(ImportSummary(imported = 7, skipped = 2, failed = 1))`
- **THEN** a snackbar appears with `key = "favorites.import.success"` and `message = getString(Res.string.favorites_import_success_with_failures, 7, 2, 1)` — i.e. `"Imported 7 favorites, skipped 2 already present — 1 failed to fetch"`

#### Scenario: Failed import due to unsupported version

- **WHEN** `repository.importFromBytes(...)` returns `Result.Error(FavoritesImportExportError.UnsupportedVersion(version = 2))`
- **THEN** a snackbar appears with `key = "favorites.import.error"` and `message = getString(Res.string.favorites_import_error_unsupported_version, 2)` — i.e. `"Failed to import: Unsupported file version (got 2, expected 1)"`

#### Scenario: Failed import due to network outage

- **WHEN** `repository.importFromBytes(...)` returns `Result.Error(FavoritesImportExportError.NetworkError(cause = "timeout"))`
- **THEN** a snackbar appears with `key = "favorites.import.error"` and `message = getString(Res.string.favorites_import_error_network)` — i.e. `"Failed to import: no network connection, please try again"`

#### Scenario: Consecutive exports both produce snackbars

- **WHEN** the user exports a file successfully
- **AND** immediately exports another file successfully
- **THEN** BOTH snackbars are emitted (the `distinctUntilChanged` operator does NOT dedupe them because the keys are stable, but the channel is processed in order)

#### Scenario: New Res.string entries are added

- **WHEN** the change is implemented
- **THEN** the 11 new resource entries listed above are added to `composeApp/src/commonMain/composeResources/values/strings.xml`
- **AND** any other locale files (`values-ru/strings.xml`, etc.) include matching entries (translated) for the user-facing strings
- **AND** no hardcoded English snackbar messages remain in the host code (the only `getString(...)` calls are for the new resources)

### Requirement: Imported favorites surface in the local pending-sync list

The system MUST mark every artist inserted by the import flow with `syncStatus = PendingRemoteAdd` in the `ArtistEntity` table. The system MUST NOT automatically push these to Supabase as part of the import — the import is local-only. The user MUST be informed (via the existing pending badge on the avatar — `AvatarState.hasPending`) that there are favorites waiting to be synced, and the user can press "Sync" to push them to remote.

The import MUST NOT trigger an automatic `syncRepository.syncRemote()` call after completion. The two flows are intentionally decoupled: import is for restoring from a backup, sync is for reconciling with the cloud. The user can compose them manually.

#### Scenario: Imported favorites show in the pending badge

- **WHEN** the user imports 5 MBIDs
- **AND** the user is NOT signed in to Supabase (or sync has not been run)
- **THEN** `AvatarState.hasPending` becomes `true` (5 rows with `syncStatus = PendingRemoteAdd` are pending)
- **AND** the badge on the avatar is rendered

#### Scenario: User presses Sync after import

- **WHEN** the user imports 5 MBIDs
- **AND** then taps the "Sync" item in the dropdown
- **THEN** `syncRepository.syncRemote()` runs as before
- **AND** the 5 imported MBIDs are pushed to Supabase
- **AND** their `syncStatus` is updated to `OK` on success

#### Scenario: Import does not auto-sync

- **WHEN** the user imports 5 MBIDs
- **THEN** no Supabase call is made as part of the import
- **AND** the user can navigate away or close the app without any network call being triggered by the import
