## Context

The project is a Kotlin Multiplatform app (Android, iOS, JVM Desktop) for tracking favorite MusicBrainz artists. Favorites are stored locally in Room and synced to a Supabase `favorite_artists` table. The user can star/unstar artists from the search page, but their list is locked to a single device. There is no way to back up, share, or move the list between devices.

The avatar dropdown on the Favorites page currently has a single "Sync" item that triggers a local-↔-remote reconciliation. The codebase already has a platform-abstracted `FileHandler` in `core/system/saveQR.kt` that handles the QR-code save flow (Android share-sheet, iOS share-sheet, JVM `FileDialog`). The QR-code scan flow on JVM also uses `java.awt.FileDialog.LOAD` in `qrCodeScanner.jvm.kt:54-83`. Neither is currently reusable for general file picker purposes — the save path uses share-sheet (wrong UX for "save to file"), and the QR-scanner is a `@Composable` (not a class), not designed for reuse.

The repository layer uses a custom `Result<T>` API (`core/resultApi.kt`) with `Result.Success` / `Result.Error(type: ErrorType, message: String?)`. Typed errors are declared as `sealed class XxxError : ErrorType()` (e.g. `SupabaseError`, `SearchError`, `SessionError`, `BrainzApiError`).

## Goals / Non-Goals

**Goals:**

- Add JSON export of the user's favorite artists list. The file is a portable, versioned JSON document the user controls.
- Add JSON import that merges MBIDs from a file into the local Room database (additive, idempotent, network-fetched for new MBIDs).
- Reuse the existing `FileHandler` `expect/actual` class for the new save/open methods, keeping the QR-code save flow and the favorites save/open flow in the same per-platform plumbing.
- Reuse the existing `Result<T>` API for repository returns, with a new typed `FavoritesImportExportError : ErrorType()` sealed class.
- Side-effect: extend `ApiArtistsRepository.addToFavorite` with a network-fallback path so the search-page "star" action no longer fails silently when the user stars an artist they have not searched for.

**Non-Goals:**

- Automatic push to Supabase after import. The import is local-only; the user presses Sync manually.
- Replacing the local favorites list from a file. The import is additive only — it never removes favorites.
- Exporting full artist metadata (name, country, lifespan, etc.). The export contains only MBIDs; the import re-fetches metadata from MusicBrainz.
- Importing from a URL or remote source. The import is file-based only.
- A dedicated iOS permissions / Android `READ_EXTERNAL_STORAGE` request. `UIDocumentPickerViewController` and `ActivityResultContracts.OpenDocument` handle permissions internally via SAF / iOS document picker.
- Changing the Supabase schema. The import is local-only.
- A schema migration for Room. `ArtistEntity` is unchanged; `OnConflictStrategy.REPLACE` handles the "flip from non-favorite" case.

## Decisions

### D1. Reuse the existing `FileHandler` rather than introduce a sibling `FilePicker` class

**Why:** The JVM side of the codebase already uses `java.awt.FileDialog` for both save (`saveQR.kt`) and load (`qrCodeScanner.jvm.kt:54-83`). Extending `FileHandler` with `saveFile` and `openFile` keeps the per-platform plumbing in one place and lets the QR-code save flow and the favorites save/open flow share the same Koin bean.

**Alternatives considered:**

- New sibling `FavoritesFileAccess` `expect/actual` class. Cleaner separation but duplicates the `FileDialog` boilerplate on JVM (3 actuals: QR save, QR scan, favorites).
- Refactor `FileHandler` to a generic `saveBytes(suggestedName, mimeType, data): String?` / `openBytes(mimeType): Pair<String, ByteArray>?` interface with `saveQR` as a thin wrapper. Larger refactor; the QR code path is single-purpose (share-sheet on Android/iOS, not a real file picker), so a generic interface is the wrong abstraction for it.

**Chosen:** extend `FileHandler` with `saveFile` and `openFile`; keep `saveQR` unchanged. The existing `saveQR` JVM actual continues to call `Desktop.getDesktop().open(file)` after writing; the new `saveFile` JVM actual does NOT call `Desktop.open` (a JSON file is not a viewer-launchable artifact).

### D2. Extend `addToFavorite` with network fallback rather than write a new `importAsFavorite` method

**Why:** The current `addToFavorite` early-returns if the artist is not in the local cache (`searchEngine.getArtistFromCache(artistId) ?: return`). The search-page "star" action silently fails for an artist the user has never searched for. Adding a network fallback to the same method fixes this for both the import flow and the search page, eliminating a code path.

**Alternatives considered:**

- New `importAsFavorite(artistId: String): Result<Unit>` method on `ArtistsRepository`. Keeps the import and the search-page flows separate, but leaves the search-page bug in place.
- Add a new public `fetchAndCacheArtist(artistId: String): Artist?` method on `SearchEngine` and have both `addToFavorite` and the import flow call it explicitly. More layers, more boilerplate.

**Chosen:** extend `addToFavorite` with `searchEngine.getArtistFromCache(artistId) ?: searchEngine.getArtistById(artistId) ?: return`. `getArtistById` is a new network-fetching method on `SearchEngine` (returns null on 404/5xx). The import flow calls `addToFavorite` for each new MBID; the search-page flow benefits transparently.

### D3. Chunk the import at 100 IDs per batched MusicBrainz request

**Why:** 100 IDs is the same limit used by the existing `ApiReleasesRepository.syncReleases` for `arid:(...)` queries. Batched queries are dramatically faster than per-ID queries at scale: 1000 IDs in 10 requests vs 1000 requests. The rate-limited `RateLimitedApiQueue` (1 req/sec) is the bottleneck either way, but the chunk size determines how long the user waits: 1000 IDs = 10 sec (10 chunks × 1 sec/chunk) vs 1000 sec (1000 per-ID requests).

**Alternatives considered:**

- 50 IDs per chunk (matches `PagingDefaults.API_PAGE`). 50 is the network-paging page size, not the batched-query limit. Smaller chunks = more requests = more time.
- 200 IDs per chunk. MusicBrainz's `arid:(...)` query has a server-side limit (typically 100-400 IDs depending on the index). 100 is the safe default.

**Chosen:** 100 IDs per chunk. Last chunk may be smaller.

### D4. `flatMapMerge(concurrency = 50)` for chunk launching

**Why:** 50 is `PagingDefaults.API_PAGE` (the existing convention in the codebase for network concurrency). 50 chunk-coroutines can be in flight at once; the `RateLimitedApiQueue` serialises the actual HTTP calls to 1 req/sec. The 50-concurrent cap is for coroutine management, not for rate-limiting — the queue handles the rate.

**Alternatives considered:**

- 5 concurrent chunks. Wastes the queue's capacity. The queue already serialises, so launching 5 vs 50 doesn't change the per-second request rate, only how many coroutines are sitting in the queue.
- 100 concurrent chunks. Matches the chunk size but is arbitrary.

**Chosen:** 50, matching `PagingDefaults.API_PAGE`.

### D5. Use the existing `Result<T>` API for repository returns

**Why:** The codebase has a custom `Result<T>` (in `core/resultApi.kt`) with `Result.Success` / `Result.Error(type: ErrorType, message: String?)`. Existing repositories (`DefaultSupabaseDb`, `ApiSyncRepository`, etc.) already use this pattern. The user's request was explicit: "use my own Result API for repository returns... the result itself must be the Result.Success and for typed errors - Result.Error."

**Alternatives considered:**

- Throw exceptions for errors. Inconsistent with the existing pattern; requires the host to wrap every call in `try/catch`.
- Return raw types and a separate error channel. Complicates the API; the existing `Result` is more ergonomic.

**Chosen:** `Result<T>` everywhere. New typed error `FavoritesImportExportError : ErrorType()` (matching `SupabaseError` / `SearchError` / `SessionError` / `BrainzApiError`).

### D6. Per-MBID failures in the success payload, "all failed" as a typed error

**Why:** A file with 5 MBIDs where 2 return 404 is a partial success — 3 were imported, 2 were not. The user wants to know "3 imported, 2 failed" — not "the whole import failed." This matches the existing `ApiSyncRepository.syncRemote()` pattern, which accumulates per-row errors in an `ErrorType` list and continues.

The "all failed" case (no MBID imported, no MBID skipped, all new MBIDs failed) is treated as a fatal error: `Result.Error(NetworkError(cause))`. This lets the host show a "check your connection" snackbar rather than a misleading "Imported 0, skipped 0 — N failed" success message. The most common cause of "all failed" is no network.

**Alternatives considered:**

- Any failure → `Result.Error`. Loses the "some imported, some failed" granularity.
- No "all failed" rule. User sees "Imported 0 favorites, skipped 0 — 100 failed to fetch" success snackbar, which is confusing.

**Chosen:** partial success → `Result.Success(ImportSummary(imported, skipped, failed))`; all-failed → `Result.Error(NetworkError(cause))`.

### D7. `FavoritesImportExportRepository` does not depend on `FileHandler`

**Why:** The repository works in bytes, not paths. The host reads the file (via `FileHandler.openFile`) and writes the file (via `FileHandler.saveFile`); the repository just serialises and parses bytes. This separation makes the repository testable (no platform code) and keeps the data layer focused.

**Alternatives considered:**

- Repository takes a path and reads/writes the file itself. Couples the repository to platform file I/O. The `FavoritesHost` already has the `FileHandler` and is the natural place to do file I/O orchestration.

**Chosen:** the repository depends on `ArtistDao`, `FavoriteDao`, and `SearchEngine` only. The host wires the `FileHandler`.

### D8. Snackbar messages via `Res.string`, not hardcoded English

**Why:** The project already uses Compose Multiplatform resources (`composeApp/src/commonMain/composeResources/values/strings.xml` + locale variants). The new snackbar messages follow the same pattern. The 11 new entries are added in the same change as the rest of the implementation.

**Alternatives considered:**

- Hardcoded English strings in the host. Inconsistent with the rest of the codebase; no localization.

**Chosen:** 11 new `Res.string` entries, with the host reading them via `getString(Res.string.X, args...)`.

## Risks / Trade-offs

**[Risk] The `addToFavorite` network-fallback change is a behaviour change visible to the search page.** Previously, starring an uncached artist was a silent no-op. Now it triggers a network request. The user sees a brief spinner. → **Mitigation:** the existing `addToFavorite` is wrapped in the same `viewModelScope` and shows the same sync indicator. The change is strictly additive — no search-page code needs to change.

**[Risk] The iOS `UIDocumentPickerViewController` import bridge is a coroutine-bridging exercise.** `suspendCancellableCoroutine` is needed to wrap the async picker result. → **Mitigation:** this is the standard pattern for iOS picker bridging in Kotlin Multiplatform; the same pattern is used in the existing `saveQR.ios.kt` for `UIActivityViewController`.

**[Risk] The Android `ActivityResultContracts.CreateDocument` flow is used for the first time in the codebase.** No prior reference. → **Mitigation:** the contract is documented in the Android Jetpack docs; the implementation follows the standard `rememberLauncherForActivityResult` pattern (already used in `qrCodeScanner.android.kt:34-35` for `RequestPermission`).

**[Risk] A "no network" import returns `Result.Error(NetworkError)` but the actual cause could be something else** (e.g. all 100 queried artists happen to be deleted from MusicBrainz — the batched response is empty, the user's "import all my favorites" produces 0 imported, 0 skipped, 100 failed). → **Mitigation:** the `NetworkError` snackbar says "no network connection, please try again" which is misleading in the "all deleted" case, but the alternative (showing "Imported 0, skipped 0 — 100 failed" success snackbar) is also misleading. The "all failed" rule trades off between two imperfect outcomes; the user can verify their network status independently. If this becomes a real issue, the rule can be tightened to "all-failed AND a transient error was observed" in a follow-up.

**[Risk] Cancellation mid-import leaves the user with a partial DB and no snackbar.** If the user navigates away from the Favorites page during an import, the `componentContext.coroutineScope()` is cancelled; the in-flight `flatMapMerge` coroutines are cancelled; the snackbar channel is unreachable. → **Mitigation:** the import is additive, so the partial DB is not a destructive state. The user can re-import the same file (idempotent re-import — already-present rows are counted as `skipped`).

**[Risk] `FileHandler.saveFile` and `FileHandler.openFile` are added to the `expect/actual` class without a way to test the picker UI in unit tests.** The JVM `FileDialog` blocks the EDT; the Android `ActivityResultContracts` require an `Activity`; the iOS `UIDocumentPickerViewController` requires a `UIViewController`. → **Mitigation:** the picker actuals are not unit-tested (matching the existing `saveQR` actuals — none of them have unit tests). The repository's byte-level logic is unit-tested in `jvmTest/`. The picker plumbing is covered by manual / integration testing on each platform.

**[Risk] The `getArtistById` method on `SearchEngine` is a new network call that may rate-limit the user.** Each call is a single MusicBrainz artist query, which counts against the 1 req/sec rate limit enforced by `RateLimitedApiQueue`. → **Mitigation:** the queue already serialises all MusicBrainz calls, so the user cannot exceed 1 req/sec regardless. The new method is on the same rate-limit path as the existing `searchEngine.getArtistFromCache`.

## Migration Plan

This change is additive only — no schema migration, no breaking API changes, no data loss. Deploy order:

1. **Code:** add the new files, modify the existing files. No migration step.
2. **Strings:** add 11 new `Res.string` entries to `composeApp/src/commonMain/composeResources/values/strings.xml` and any locale variants (`values-ru/strings.xml`, etc.) — translated where applicable.
3. **KSP:** the new `@Single(binds = [FavoritesImportExportRepository::class])` is auto-discovered by `@ComponentScan`. No new Koin module edits.
4. **Supabase:** no changes. The import feature does not write to Supabase.
5. **Room:** no migration. `ArtistEntity` is unchanged; `OnConflictStrategy.REPLACE` handles the "flip from non-favorite" case.

Rollback: revert the commit. No data is migrated in either direction, so the rollback is clean.

## Open Questions

None — all design decisions are resolved (see the proposal and the spec for the full set of resolved decisions).
