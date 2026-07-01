## Why

The user can favorite artists from the search page, but their list is locked to a single device. If they reinstall, switch phones, or want to back up their work, the list is gone. We need a portable file format the user controls so they can move their favorites between devices, share with friends, and back up.

## What Changes

- **Add JSON export** of the user's favorite artists list. The file contains a versioned, forward-compatible JSON document with a `format` identifier, integer `version`, `exportedAt` timestamp, and a list of MusicBrainz IDs (MBIDs). The user picks a destination via the platform file picker.
- **Add JSON import** that merges the MBIDs from a file into the local Room database. Already-present favorites are skipped; new MBIDs are batch-fetched from MusicBrainz (100 IDs per batched request, `flatMapMerge(concurrency = 50)`, rate-limited to 1 req/sec by the existing `RateLimitedApiQueue`) and added as favorites with `syncStatus = PendingRemoteAdd`. After import, the user can press Sync to push the new favorites to Supabase.
- **Extend the existing `FileHandler` `expect/actual` class** (in `core/system/saveQR.kt`) with two new suspend methods — `saveFile(suggestedName, mimeType, data): String?` and `openFile(mimeType): Pair<String, ByteArray>?` — so the QR-code save flow and the favorites save/open flow share the same per-platform plumbing. The existing `saveQR` method is unchanged. JVM actuals reuse `java.awt.FileDialog` (mirroring `qrCodeScanner.jvm.kt:54-83`); Android gets new `ActivityResultContracts.CreateDocument` and `OpenDocument` actuals; iOS gets new `UIDocumentPickerViewController` actuals using `UTType.json` (project deploys to iOS 18.2).
- **Extend `ApiArtistsRepository.addToFavorite`** with a network-fallback path: if the artist is not in the local cache, the method now calls `searchEngine.getArtistById(artistId)` (network fetch) before falling back to no-op. This is a side-effect of the import feature: previously the search-page "star" action failed silently when the user starred an artist they had not searched for. The fix benefits both flows.
- **Add two new `DropdownMenuItem`s** to the existing avatar dropdown in the Favorites page top bar — "Import favorites" and "Export favorites" — below the existing "Sync" item. The existing `Avatar` composable is extended with `onImportRequest: () -> Unit` and `onExportRequest: () -> Unit` callbacks.
- **Add typed error hierarchy** `sealed class FavoritesImportExportError : ErrorType()` (in `musicBrainz/domain/FavoritesImportExportError.kt`, matching the existing `SupabaseError` / `SearchError` / `SessionError` / `BrainzApiError` pattern in `core/`) — covers `DataReadError` (export), `Malformed` / `UnsupportedFormat` / `UnsupportedVersion` / `MissingArtistsField` / `EmptyArtistEntry` (import validation), and `NetworkError` (all-failed import).
- **Use the project's existing `Result<T>` API** for repository returns (`core/resultApi.kt`). The repository never throws on application-level errors; the host unwraps via `forSuccess` / `forError`. Per-MBID failures (one artist 404s, another times out) are NOT typed errors — they are partial outcomes reported in the `ImportSummary.failed` count. A file where every new MBID failed (likely no network) is `Result.Error(NetworkError)`.
- **Add new state fields** `isImporting: Boolean` and `isExporting: Boolean` to `FavoritesHostState` (the `@Serializable` state held by `saveableMutableValue`), with default `false` so existing saved states remain valid. The new dropdown items are disabled while the corresponding flag is `true`.
- **Add new actions** `ImportRequested` and `ExportRequested` to `FavoritesHostAction`. The `invoke` handler unwraps the repository `Result` and emits snackbars via the existing `FavoritesHost.events: Channel<SnackbarMsg>` channel.
- **Add 11 new `Res.string` resource entries** for the snackbar messages (all 4 keys: `favorites.export.success`, `favorites.export.error`, `favorites.import.success`, `favorites.import.error`).

## Capabilities

### New Capabilities

- `favorites-import-export`: The full set of behaviors described in the spec — versioned JSON file format, platform-abstracted file picker via the extended `FileHandler`, batched-MusicBrainz import with typed `Result<T>` error handling, and the 3-item avatar dropdown UI. This is the spec being created at `openspec/specs/favorites-import-export/spec.md`.

### Modified Capabilities

- `koin-annotations`: No spec-level requirement changes. The new `@Single(binds = [FavoritesImportExportRepository::class])` annotation is auto-discovered by the existing `@ComponentScan` on `AppModule` and `DbModule`, and `FileHandler` is already in the Koin graph. No new module-level provider functions are added. The spec is unaffected.

## Impact

- **New files**:
  - `composeApp/src/commonMain/.../musicBrainz/domain/FavoritesImportExportRepository.kt` — repository with `serializeExport()` and `importFromBytes()`, plus `FavoritesImportExportPayload` and `ImportSummary` data classes.
  - `composeApp/src/commonMain/.../musicBrainz/domain/FavoritesImportExportError.kt` — the typed error sealed class.
  - JUnit tests for the repository in `composeApp/src/jvmTest/`.

- **Modified files**:
  - `composeApp/src/commonMain/.../core/system/saveQR.kt` — `expect class FileHandler` gains `saveFile` and `openFile` declarations.
  - `composeApp/src/jvmMain/.../core/system/saveQR.kt` — JVM actual for `saveFile` (reuses `java.awt.FileDialog.SAVE`) and `openFile` (reuses `java.awt.FileDialog.LOAD` with `.json` filter, mirroring `qrCodeScanner.jvm.kt:54-83`). Existing `saveQR` is unchanged.
  - `composeApp/src/androidMain/.../core/system/saveQR.android.kt` — adds Android actuals for `saveFile` (using `ActivityResultContracts.CreateDocument` for the first time in the codebase) and `openFile` (using `ActivityResultContracts.OpenDocument`).
  - `composeApp/src/iosMain/.../core/system/saveQR.ios.kt` — adds iOS actuals for `saveFile` (`UIDocumentPickerViewController` in `forExporting` mode) and `openFile` (`UIDocumentPickerViewController` with `forOpeningContentTypes = [UTType.json]`). Uses `suspendCancellableCoroutine` to bridge the picker result. Existing `saveQR` is unchanged.
  - `composeApp/src/commonMain/.../musicBrainz/data/repository/ApiArtistsRepository.kt` — `addToFavorite` gains a network-fallback path: `searchEngine.getArtistFromCache(artistId) ?: searchEngine.getArtistById(artistId) ?: return`.
  - `composeApp/src/commonMain/.../musicBrainz/data/remote/ApiSearchEngine.kt` — new `getArtistById(artistId: String): Artist?` method (network fetch, returns null on 404 / 5xx). Used by `addToFavorite` for the network fallback and by the import flow's batched queries.
  - `composeApp/src/commonMain/.../musicBrainz/data/remote/ApiSearchEngine.kt` — new `searchArtistsByIdBatch(ids: List<String>): List<ArtistDto>` method (batched MusicBrainz query, mirroring `ApiReleasesRepository.syncReleases`'s `arid:(...)` pattern).
  - `composeApp/src/commonMain/.../screens/mainScreen/pages/favorites/domain/pageHost/FavoritesHostAction.kt` — adds `ImportRequested` and `ExportRequested` data objects.
  - `composeApp/src/commonMain/.../screens/mainScreen/pages/favorites/domain/pageHost/FavoritesHostState.kt` — adds `isImporting` and `isExporting` fields with `false` defaults.
  - `composeApp/src/commonMain/.../screens/mainScreen/pages/favorites/domain/pageHost/FavoritesHost.kt` — constructor gains `FileHandler` and `FavoritesImportExportRepository` dependencies; `invoke` handler processes the two new actions via `forSuccess` / `forError`.
  - `composeApp/src/commonMain/.../screens/mainScreen/pages/favorites/ui/components/nicknameAvatar/Avatar.kt` — `Avatar` composable gains `onImportRequest` and `onExportRequest` callbacks; `DropdownMenu` adds the two new items below the existing "Sync" item.
  - `composeApp/src/commonMain/.../screens/mainScreen/pages/favorites/ui/FavoriteHostSlots.kt` — wires the new callbacks to `FavoritesHostAction.ImportRequested` and `FavoritesHostAction.ExportRequested`.
  - `composeApp/src/commonMain/composeResources/values/strings.xml` (+ locale variants) — adds 11 new `Res.string` entries for the snackbar messages.

- **New strings** (no new Koin modules; `@ComponentScan` discovers the new `@Single` automatically):
  - 11 new `Res.string` keys (listed in the spec).

- **KSP**: no new KSP processors required. The new `@Single` is auto-discovered by the existing Koin compiler plugin. Room schema is unchanged (no new entities, no new queries in `FavoriteDao` or `ArtistDao`).

- **iOS**: no new permissions in `Info.plist` — `UIDocumentPickerViewController` does not require special entitlements. `UTType.json` is available because the project deploys to iOS 18.2.

- **Android**: no new permissions. `ActivityResultContracts.CreateDocument` and `OpenDocument` do not require `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` (the contracts handle the SAF interaction internally).

- **Supabase schema**: no changes. The import flow is local-only; Supabase is not touched. The user can press Sync afterwards to push the new favorites to remote via the existing `ApiSyncRepository.syncRemote()`.

- **Database migrations**: no schema migration. `ArtistEntity` is unchanged; `OnConflictStrategy.REPLACE` handles the "flip from non-favorite" case in the existing `dbRepository.insertArtist` call.

- **Out of scope**: automatic sync to Supabase after import (the user does it manually); replacing the favorites list (the import is additive only); exporting full artist metadata (only MBIDs are exported); bulk-delete of favorites from a file (the import never removes existing favorites).
