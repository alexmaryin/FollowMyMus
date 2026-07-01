## 1. Typed error hierarchy

- [x] 1.1 Create `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/musicBrainz/domain/FavoritesImportExportError.kt` with the `sealed class FavoritesImportExportError : ErrorType()` hierarchy (7 cases: `DataReadError`, `Malformed`, `UnsupportedFormat`, `UnsupportedVersion`, `MissingArtistsField`, `EmptyArtistEntry`, `NetworkError`). Import the `ErrorType` from `core/resultApi.kt`. Match the style of the existing `SupabaseError` / `SearchError` / `SessionError` sealed classes.

## 2. Search engine network methods

- [x] 2.1 In `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/musicBrainz/data/remote/ApiSearchEngine.kt`, add `suspend fun getArtistById(artistId: String): Artist?`. The implementation MUST call `GET /ws/2/artist/{id}?inc=...&fmt=json` via the existing Ktor client, return the parsed `ArtistDto` (mapped to the domain `Artist` model) on 200, return `null` on 404, and return `null` on 5xx / network error. Route through the existing `RateLimitedApiQueue` so the 1 req/sec cap is respected.
- [x] 2.2 In the same file, add `suspend fun searchArtistsByIdBatch(ids: List<String>): List<ArtistDto>`. The implementation MUST issue `GET /ws/2/artist?query=arid:(id1 OR id2 OR ... OR idN)&limit=100&fmt=json` for the given list of IDs (caller is responsible for chunking at 100). Returns the parsed `ArtistDto` list. The IDs are sorted into a deterministic order before the query so the `arid:(...)` clause is stable. Route through `RateLimitedApiQueue`.
- [x] 2.3 Add a constant `SearchEngine.BATCH_LIMIT = 100` in the `SearchEngine` interface so the batch size is named.

## 3. Extend addToFavorite with network fallback

- [x] 3.1 In `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/musicBrainz/data/repository/ApiArtistsRepository.kt`, replace the `addToFavorite` body's cache lookup with: `val artist = searchEngine.getArtistFromCache(artistId) ?: searchEngine.getArtistById(artistId) ?: return`. The rest of the method (insert + supabase upsert) is unchanged. This is a side-effect of the favorites-import-export feature that also benefits the existing search-page "star" action.

## 4. Extend FileHandler with saveFile and openFile

- [x] 4.1 In `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/core/system/saveQR.kt`, add two new `suspend` method declarations to the `expect class FileHandler()`: `suspend fun saveFile(suggestedName: String, mimeType: String, data: ByteArray): String?` and `suspend fun openFile(mimeType: String): Pair<String, ByteArray>?`. The existing `saveQR` declaration is unchanged.
- [x] 4.2 In `composeApp/src/jvmMain/kotlin/io/github/alexmaryin/followmymus/core/system/saveQR.kt`, add the JVM `actual` for `saveFile`: open a `java.awt.FileDialog(null as Frame?, "Save favorites", FileDialog.SAVE)`, set `fileDialog.file = suggestedName`, set `isVisible = true`, and return `fileDialog.files.firstOrNull()?.absolutePath` after `writeBytes(data)`. The actual MUST NOT call `Desktop.getDesktop().open(file)`. The existing `saveQR` actual is unchanged.
- [x] 4.3 In the same file, add the JVM `actual` for `openFile`: open a `java.awt.FileDialog(null as Frame?, "Open favorites", FileDialog.LOAD)` with a `FilenameFilter` accepting only `.json` files, set `isVisible = true`, and return `Pair(file.absolutePath, file.readBytes())` on success or `null` on cancel. Mirror the structure of `screens/login/ui/parts/qrCodeScanner.jvm.kt:54-83` (which uses `FileDialog.LOAD` for image files).
- [x] 4.4 In `composeApp/src/androidMain/kotlin/io/github/alexmaryin/followmymus/core/system/saveQR.android.kt`, add the Android `actual` for `saveFile` and `openFile`. Use `ActivityResultContracts.CreateDocument` and `ActivityResultContracts.OpenDocument` respectively, registered through the existing `Context` injection (`KoinComponent` + `inject<Context>()`). Use `suspendCancellableCoroutine` to bridge the picker result. Content URIs are read via `contentResolver.openInputStream(uri)` (open) and written via `contentResolver.openOutputStream(uri)` (save). The existing `saveQR` actual (which uses `Intent.ACTION_SEND`) is unchanged.
- [x] 4.5 In `composeApp/src/iosMain/kotlin/io/github/alexmaryin/followmymus/core/system/saveQR.ios.kt`, add the iOS `actual` for `saveFile` and `openFile`. Use `UIDocumentPickerViewController` in `forExporting` mode for save and with `forOpeningContentTypes = [UTType.json]` for open. Bridge through `platform.UIKit` and `platform.UniformTypeIdentifiers`. Use `suspendCancellableCoroutine` to wait for the picker result. The existing `saveQR` actual (which uses `UIActivityViewController`) is unchanged.

## 5. FavoritesImportExportRepository

- [x] 5.1 Create `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/musicBrainz/domain/FavoritesImportExportRepository.kt` with the `FavoritesImportExportRepository` class and the data classes `FavoritesExportPayload(bytes: ByteArray, count: Int)` and `ImportSummary(imported: Int, skipped: Int, failed: Int)`. Register as `@Single(binds = [FavoritesImportExportRepository::class])`. Constructor: `artistDao: ArtistDao`, `favoriteDao: FavoriteDao`, `searchEngine: SearchEngine`, `artistsRepository: ArtistsRepository`. Use the project's `Json` bean from Koin (resolve via `get()` since `FavoritesImportExportRepository` extends `KoinComponent`, or inject as a constructor parameter).
- [x] 5.2 Implement `suspend fun serializeExport(): Result<FavoritesExportPayload>`. Read `favoriteDao.getFavoriteArtistsIds().first()`, sort lexicographically, wrap in `FavoritesExportFile(format = "followmymus.favorites", version = 1, exportedAt = Clock.System.now().toString(), artists = sorted)`, encode via `Json.encodeToString(...)` to UTF-8 bytes, and return `Result.Success(FavoritesExportPayload(bytes, count = sorted.size))`. On any throwable from Room, catch and return `Result.Error(FavoritesImportExportError.DataReadError(cause = e.message))`. Use `withContext(Dispatchers.IO)` for the DB read.
- [x] 5.3 Implement `suspend fun importFromBytes(bytes: ByteArray, sourceName: String? = null): Result<ImportSummary>`. The implementation MUST:
  - Decode the bytes with `Json` and validate `format` / `version` / `artists` per the spec. Return `Result.Error(Malformed / UnsupportedFormat / UnsupportedVersion(N) / MissingArtistsField / EmptyArtistEntry(i))` on validation failure.
  - Deduplicate the `artists` list (`artists.toSet().toList()`).
  - Get the set of already-favorite MBIDs via `favoriteDao.getFavoriteArtistsIds().first()`. Count those into `skipped`; the rest are `new`.
  - Chunk `new` into batches of `SearchEngine.LIMIT` (100). For each chunk, call `searchEngine.searchArtistsByIdBatch(chunk)` via `flatMapMerge(concurrency = 50)`.
  - For each `ArtistDto` returned, call `artistsRepository.addToFavorite(artist.id)`. Increment `imported` for each successful insert. Increment `failed` for each chunk-MBID that was queried but did not appear in the response (the MBID is in the chunk's query but not in the response — count it as `failed`). If the batched request itself throws, increment `failed` for every MBID in the chunk and capture the first error message.
  - After all chunks: if `imported == 0 && skipped == 0 && failed > 0`, return `Result.Error(NetworkError(cause = <first error>))`. Otherwise return `Result.Success(ImportSummary(imported, skipped, failed))`.
  - Run the `flatMapMerge` inside `coroutineScope` so cancellation propagates cleanly.
  - Honour coroutine cancellation.

## 6. Snackbar string resources

- [x] 6.1 In `composeApp/src/commonMain/composeResources/values/strings.xml`, add the 11 new resource entries listed in the spec (`favorites_export_success`, `favorites_export_error_data_read`, `favorites_import_success_clean`, `favorites_import_success_with_failures`, `favorites_import_error_malformed`, `favorites_import_error_unsupported_format`, `favorites_import_error_unsupported_version`, `favorites_import_error_missing_artists`, `favorites_import_error_empty_artist_entry`, `favorites_import_error_network`, `favorites_import_error_unknown`). The English values are in the spec.
- [x] 6.2 In any other locale files (`values-ru/strings.xml`, etc.), add matching entries (translated) for the 11 new keys. If a locale file is missing, leave it for a follow-up — only `values/strings.xml` is required for the change.

## 7. Action and state extensions

- [x] 7.1 In `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/screens/mainScreen/pages/favorites/domain/pageHost/FavoritesHostAction.kt`, add two new `data object` cases: `ImportRequested` and `ExportRequested`.
- [x] 7.2 In `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/screens/mainScreen/pages/favorites/domain/pageHost/FavoritesHostState.kt`, add two new boolean fields with `false` defaults: `isImporting: Boolean = false` and `isExporting: Boolean = false`. The existing fields and `@Serializable` annotation are unchanged.

## 8. Wire FavoritesHost orchestration

- [x] 8.1 In `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/screens/mainScreen/pages/favorites/domain/pageHost/FavoritesHost.kt`, add two new constructor parameters: `private val fileHandler: FileHandler` and `private val favoritesImportExportRepository: FavoritesImportExportRepository`. Both are resolved from Koin via the existing `@Factory` annotation. The existing `syncRepository` constructor parameter is unchanged.
- [x] 8.2 In the same file, add a new branch in `invoke(action: FavoritesHostAction)` for `FavoritesHostAction.ExportRequested`. The handler MUST:
  - Set `_state.update { it.copy(isExporting = true) }`.
  - Call `favoritesImportExportRepository.serializeExport()` inside `try { } finally { isExporting = false }`.
  - On `forSuccess { payload -> ... }`: call `fileHandler.saveFile("favorites-${today}.json", "application/json", payload.bytes)`. If non-null, emit `SnackbarMsg(key = "favorites.export.success", message = getString(Res.string.favorites_export_success, payload.count))`.
  - On `forError { type, _ -> ... }`: emit `SnackbarMsg(key = "favorites.export.error", message = getString(Res.string.favorites_export_error_data_read))`.
  - Catch any unexpected `Throwable` and emit `Res.string.favorites_export_error_unknown` (use the same key as the typed error).
- [x] 8.3 In the same file, add a new branch for `FavoritesHostAction.ImportRequested`. The handler MUST:
  - Set `_state.update { it.copy(isImporting = true) }`.
  - Call `fileHandler.openFile("application/json")` inside `try { } finally { isImporting = false }`. If null (cancelled), return without emitting a snackbar.
  - On non-null `Pair(path, bytes)`, call `favoritesImportExportRepository.importFromBytes(bytes, sourceName = path)`.
  - On `forSuccess { summary -> ... }`: emit the success snackbar (`favorites_import_success_clean` if `summary.failed == 0`, else `favorites_import_success_with_failures`).
  - On `forError { type, _ -> ... }`: map the typed error to the appropriate `Res.string.favorites_import_error_*` and emit the snackbar.
  - Catch any unexpected `Throwable` and emit `Res.string.favorites_import_error_unknown`.

## 9. Avatar composable dropdown

- [x] 9.1 In `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/screens/mainScreen/pages/favorites/ui/components/nicknameAvatar/Avatar.kt`, add two new `onImportRequest: () -> Unit` and `onExportRequest: () -> Unit` callback parameters to the `Avatar` composable. Add the two new `DropdownMenuItem`s to the existing `DropdownMenu` (below the existing "Sync" item, above any existing dividers if present). Each item MUST be `enabled = !state.isImporting` (for Import) and `enabled = !state.isExporting` (for Export). The dropdown MUST contain exactly three items: Sync, Import favorites, Export favorites.

## 10. Wire the new dropdown callbacks

- [x] 10.1 In `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/screens/mainScreen/pages/favorites/ui/FavoriteHostSlots.kt`, update the `Avatar` call in the `trailingIcon` slot to pass `onImportRequest = { component(FavoritesHostAction.ImportRequested) }` and `onExportRequest = { component(FavoritesHostAction.ExportRequested) }`. The existing `onSyncRequest` is unchanged.

## 11. Tests

- [x] 11.1 Add unit tests for `FavoritesImportExportRepository` in `composeApp/src/jvmTest/`. Use MockK for the `SearchEngine` and `ArtistDao` / `FavoriteDao` dependencies (or in-memory Room for the DAOs). Test cases:
  - `serializeExport` with N favorites returns `Result.Success(FavoritesExportPayload(bytes, count = N))` with lex-sorted MBIDs.
  - `serializeExport` with 0 favorites returns `Result.Success(FavoritesExportPayload(bytes, count = 0))` with empty `artists` array.
  - `serializeExport` when Room throws returns `Result.Error(DataReadError(...))`.
  - `importFromBytes` with valid file and 5 new MBIDs returns `Result.Success(ImportSummary(5, 0, 0))` and `addToFavorite` is called for each.
  - `importFromBytes` with 3 already-favorite MBIDs returns `Result.Success(ImportSummary(0, 3, 0))` and NO MusicBrainz fetch is made.
  - `importFromBytes` with mixed (2 already, 3 new) returns `Result.Success(ImportSummary(3, 2, 0))`.
  - `importFromBytes` with 5 new MBIDs where 2 are deleted (404) returns `Result.Success(ImportSummary(3, 0, 2))`.
  - `importFromBytes` with 5 new MBIDs and no network (all chunks fail) returns `Result.Error(NetworkError(...))`.
  - `importFromBytes` with `version = 2` returns `Result.Error(UnsupportedVersion(2))`.
  - `importFromBytes` with `format = "other.app"` returns `Result.Error(UnsupportedFormat)`.
  - `importFromBytes` with non-JSON bytes returns `Result.Error(Malformed)`.
  - `importFromBytes` with missing `artists` field returns `Result.Error(MissingArtistsField)`.
  - `importFromBytes` with empty string in `artists` returns `Result.Error(EmptyArtistEntry(index))`.
  - `importFromBytes` with duplicate MBIDs deduplicates correctly.
  - `importFromBytes` with 250 new MBIDs chunks into 3 batches (100, 100, 50).
  - The `sourceName` parameter appears in `Result.Error.message` for `UnsupportedVersion` etc.
- [x] 11.2 Add a test for `ApiArtistsRepository.addToFavorite` with the new network fallback: when `getArtistFromCache` returns null and `getArtistById` returns an `Artist`, the insert + supabase upsert are called. When both return null, the method is a no-op.
- [x] 11.3 Add a test for `ApiSearchEngine.getArtistById` and `searchArtistsByIdBatch` (HTTP-level test using MockK on the Ktor client, or skip if covered by existing search engine tests).

## 12. Build and verification

- [x] 12.1 Run `./gradlew clean :composeApp:assembleDebug` to ensure KSP processes the new `@Single` annotation, regenerates `AppModule.module` / `DbModule.module`, and compiles cleanly across all three source sets (commonMain, androidMain, iosMain, jvmMain).
- [x] 12.2 Run `./gradlew :composeApp:jvmTest` to verify the unit tests pass.
- [x] 12.3 Run `./gradlew :androidApp:lint` to check for any lint issues introduced by the change.
- [x] 12.4 Manual smoke test on each platform:
  - **Android**: tap avatar → "Export favorites" → pick a Downloads location → file appears. Tap avatar → "Import favorites" → pick the file → favorites appear in the list.
  - **iOS**: same flow on iPhone / iPad simulator.
  - **JVM**: same flow on desktop. Verify the suggested filename is `favorites-2026-07-01.json` and the file filter shows only `.json`.
- [x] 12.5 Verify the existing `AccountPage.DownloadQR` action still works (the `saveQR` actuals are unchanged, but the shared `FileHandler` class is modified — confirm no regression).
- [x] 12.6 Verify the existing `search page → star an uncached artist` flow now triggers a network fetch (previously a silent no-op). The artist should appear as a favorite after the fetch completes.

### Bug fixes discovered during manual testing
- **Logout now clears ALL Room tables**: `RoomRepository.clearLocalData()` now also clears `new_releases`, `MediaItemEntity`, `TrackEntity`, and `MediaResourceEntity` (previously leaked across logins). Added `NewReleasesDao.clear()` and clear methods for media child entities to `MediaDao`.
- **Import uses batch operations**: Replaced per-DTO `addToFavorite()` calls in `FavoritesImportExportRepository.importFromBytes()` with a single `addToFavoritesBulk()` call after all DTOs are fetched. The new `ArtistsRepository.addToFavoritesBulk(artists)` uses `LocalDbRepository.bulkInsertArtists()` + `SupabaseDb.bulkAddFavoriteArtists()` — one Room transaction + one Supabase upsert instead of N individual calls.

