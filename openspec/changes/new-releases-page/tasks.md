## 0. Coding style guide

This change follows a **functional-first Kotlin** style. Apply it to all new code; existing code is grandfathered. The goal is compositions of pure functions and `Flow` combinators, not imperative state machines. Violations are fine when a more imperative form is genuinely clearer — add a one-line comment explaining why.

**Data and control flow**
- `val` over `var`. Mutable state is allowed only at well-defined boundaries (the `WorkState` `MutableStateFlow`, a single owner).
- Expression bodies for one-liners; blocks for anything that needs a name or comment.
- Destructuring `val (a, b) = pair` over `pair.first; pair.second`.
- `Pair`/`Triple` for ad-hoc tuples; introduce a `data class` only for 4+ fields or behavior.
- Sealed types over enums-with-state or nulls when variants carry different payloads.
- `?.`, `?:`, `takeIf`, `takeUnless` over nested `if (x != null)`.

**Errors**
- `runCatching { }` for non-suspending work; for suspending work, a small `suspendRunCatching { }` helper (or the existing project pattern) over `try/catch` repetition.
- `Result.fold` / `.onSuccess` / `.onFailure` over `try/catch` + manual flag.
- No `!!` unless the invariant is provable and documented inline.

**Flows**
- `Flow.combine` to merge multiple reactive sources, then `.map` / `.filter` / `.flatMapLatest` to derive the output.
- `.first()` / `.firstOrNull()` for one-shot reads; `.collect` only when state is genuinely streaming.
- `flatMapLatest` over manual cancellation flags when the consumer cares only about the latest.
- `scan` / `runningFold` for stateful derivations; avoid `MutableStateFlow` updates from inside `combine { }` blocks (can deadlock or re-emit stale values).
- `flow { emit(...); emitAll(upstream) }` for complex flows; named helpers for any flow that exceeds one screen of code.

**Scope functions**
- `let` — null-checks and transformations whose result is used.
- `also` — side-effects whose return is the original.
- `apply` — object configuration.
- `run` — block-scoped computations where `this` reads cleanly.
- `takeIf` / `takeUnless` — conditional inclusion.

**Functions**
- Top-level extension functions for type-rich utilities; methods on a class only for true ownership or state.
- Function references (`::foo`) over lambdas (`{ foo() }`) when cleaner.
- Pure functions where possible; side-effects allowed at boundaries (network, disk, logging).

**Naming**
- Full words over abbreviations (`favoriteIds` over `favIds`) except established terms (DAO, DTO, KSP, etc.).
- Domain language over framework language (`sync` over `fetchWithCache`).

## 1. Schema and data model

- [x] 1.1 Bump `MusicBrainzDatabase` to version 2. Add `fallbackToDestructiveMigration` to the database builder in `appModule.kt` (or wherever the database is constructed). The bump is caused by the `new_releases` table only — the DataStore-backed settings do not participate in the Room schema.
- [x] 1.2 Add `NewReleaseState` enum (`UNSEEN`, `SEEN`, `DISMISSED`) to `musicBrainz/data/local/model/`.
- [x] 1.3 Add `NewReleaseEntity` Room entity to `musicBrainz/data/local/model/NewReleaseEntity.kt` with columns: `id` (PK, release-group MB id), `artistId`, `artistName` (denormalized), `title`, `disambiguation`, `firstReleaseDate: String?`, `primaryType: ReleaseType`, `secondaryTypes: String?` (comma-joined — matches `ReleaseEntity.secondaryTypes`, no `TypeConverter` needed), `coverFrontUrl: String?`, `state: NewReleaseState` (default `UNSEEN`), `discoveredAt: Instant`.
- [x] 1.4 Add `NewReleasesDao` to `musicBrainz/data/local/dao/NewReleasesDao.kt` with methods:
  - `getNewReleases(): PagingSource<Int, NewReleaseEntity>` (excludes `DISMISSED`, sorted by `firstReleaseDate DESC`)
  - `getUnseenCount(): Flow<Int>`
  - `upsertNewReleases(releases: List<NewReleaseEntity>)` — uses `INSERT ... ON CONFLICT DO UPDATE SET ...` for mutable fields only, preserving `state` (write `INSERT OR IGNORE` for the `state` column on conflict)
  - `markSeen(releaseId: String)`
  - `markDismissed(releaseId: String)`
- [x] 1.5 Add `@Database(entities = [...NewReleaseEntity::class], version = 2)` to `MusicBrainzDatabase`. Register `NewReleasesDao` in the `abstract class`. No `AppSettingsDao` — settings live in DataStore (see §1.6).

## 1.6 DataStore-backed app settings

- [x] 1.6.1 Add a `data class AppSettings(val newReleasesLastOpenedDay: LocalDate?, val newReleasesLastSyncCompletedAt: Instant?)` to `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/preferences/AppSettings.kt`.
- [x] 1.6.2 In the same file, add a `Flow`-typed getter as a top-level extension on `PreferenceSource`:
  ```kotlin
  fun PreferenceSource.getAppSettings(): Flow<AppSettings> =
      prefs.data.map { p ->
          AppSettings(
              newReleasesLastOpenedDay = p[stringPreferencesKey("new_releases_last_opened_day")]?.let(LocalDate::parse),
              newReleasesLastSyncCompletedAt = p[stringPreferencesKey("new_releases_last_sync_completed_at")]?.let(Instant::parse),
          )
      }.flowOn(Dispatchers.IO)
  ```
  - Both fields are nullable. A `null` `newReleasesLastOpenedDay` is the "first sync ever" signal (the floor becomes `today - 30 days` per §5.2).
  - `LocalDate` and `Instant` are from `kotlinx-datetime`. The string format is `LocalDate.toString()` / `Instant.toString()` (ISO-8601).
  - `prefs` is the `private val Prefs` inside `PreferenceSource`. Make it accessible via an `internal val` accessor (do NOT change the visibility of the field) or pass the value through a private helper. Recommended: add `internal val preferences: Prefs get() = prefs` to `PreferenceSource` so the extension can read it without leaking the field.
- [x] 1.6.3 In the same file, add a `suspend fun` setter as a top-level extension on `PreferenceSource`:
  ```kotlin
  suspend fun PreferenceSource.setNewReleasesFloor(day: LocalDate, now: Instant) {
      withContext(Dispatchers.IO) {
          try {
              prefs.edit { p ->
                  p[stringPreferencesKey("new_releases_last_opened_day")] = day.toString()
                  p[stringPreferencesKey("new_releases_last_sync_completed_at")] = now.toString()
              }
          } catch (e: Exception) {
              println("FAILED TO SAVE APP SETTINGS ON THE DISK!")
              e.printStackTrace()
          }
      }
  }
  ```
  - Mirrors the `changeThemeMode` / `changeLanguage` pattern: `withContext(Dispatchers.IO)` + `prefs.edit { }` wrapped in try/catch. No error propagation (matches the existing project convention; the sync still completes).
  - The two keys are written in a single `prefs.edit { }` block, which is atomic per DataStore's actor-based serialization. The floor and the timestamp are always updated together on success.
- [x] 1.6.4 No new Koin binding for `AppSettings`. The extension functions operate on the existing `PreferenceSource` `@Single`, which is already in the graph.

## 2. Search endpoint and DTOs

- [x] 2.1 Add `SearchReleaseGroupResponse` to `musicBrainz/data/remote/model/SearchReleaseGroupResponse.kt` with fields: `count: Int`, `releaseGroups: List<ReleaseGroupDto>` (mapped from `@SerialName("release-groups")`).
- [x] 2.2 Add `ReleaseGroupDto` to `musicBrainz/data/remote/model/ReleaseGroupDto.kt` with fields: `id: String`, `title: String`, `disambiguation: String?`, `firstReleaseDate: String?` (mapped from `@SerialName("first-release-date")`), `primaryType: ReleaseType?` (mapped from `@SerialName("primary-type")`), `secondaryTypes: List<SecondaryType>` (mapped from `@SerialName("secondary-types")` — distinct from `ReleaseType` per MB's release-group schema; the field is only populated on the full release-group resource, so the search response leaves it at `emptyList()`), `artistCredit: List<ArtistCreditDto>` (mapped from `@SerialName("artist-credit")` — the source of truth for the entity's denormalized `artistId`/`artistName`; see `ArtistCreditDto.kt` and `ArtistRefDto.kt` in the same package).
- [x] 2.3 Add `searchReleaseGroups(query: String, offset: Int, limit: Int): Result<SearchReleaseGroupResponse>` to the `SearchEngine` interface in `musicBrainz/domain/SearchEngine.kt`. The default `limit` MUST be 100 (MB's documented max for the `release-group` search endpoint).
- [x] 2.4 Implement `searchReleaseGroups` in `ApiSearchEngine`. Build the URL as `/release-group?query=...&limit=...&offset=...&fmt=json`. Reuse the existing `HttpClient` and error mapping from `searchReleases` and `searchMedia`. Set the `User-Agent` header inline on every call (matching the existing pattern).

## 3. Rate-limited API queue

- [x] 3.1 Add `RateLimitedApiQueue` to `core/network/RateLimitedApiQueue.kt`:
  - Backing field: `Channel<suspend () -> Unit>(Channel.UNLIMITED)` and a `CoroutineScope(SupervisorJob() + Dispatchers.IO)`.
  - On `init`, launch a single-consumer coroutine that loops over the channel and invokes each block with `delay(1000L)` between invocations.
  - Public API: `suspend fun <T> enqueue(block: suspend () -> T): T` — wraps the block in a `CompletableDeferred`, enqueues a lambda that runs the block and completes the deferred, then `await`s the deferred.
- [x] 3.2 Register `RateLimitedApiQueue` as a Koin `@Single` in `appModule.kt` (or a new `networkModule.kt`).
- [x] 3.3 Verify with a unit test (in 7.1) that two consecutive `enqueue` calls are spaced by ≥ 1 second.

## 4. Retrofit ApiReleasesRepository to use the queue

- [x] 4.1 Inject `RateLimitedApiQueue` into `ApiReleasesRepository`'s constructor.
- [x] 4.2 In `ApiReleasesRepository.syncReleases`, wrap each `searchEngine.searchReleases(artistId, offset, limit)` call in `rateLimitedApiQueue.enqueue { ... }`. The observable behavior is unchanged (paginates all pages, persists per page, marks `PARTIAL_SYNC` on failure); only the request pacing is enforced.

## 5. NewReleasesRepository

- [x] 5.1 Add `NewReleasesRepository` interface to `musicBrainz/domain/NewReleasesRepository.kt`:
  - `getNewReleases(): Flow<PagingData<NewReleaseEntity>>`
  - `suspend fun syncNewReleases(): Result<Unit>`
  - `val workState: StateFlow<WorkState>`
  - `val errors: Flow<BrainzApiError>`
  - `suspend fun markSeen(releaseId: String)`
  - `suspend fun markDismissed(releaseId: String)`
- [x] 5.2 Implement `ApiNewReleasesRepository` to `musicBrainz/data/repository/ApiNewReleasesRepository.kt` using the functional Kotlin style from §0. **No `AppSettingsDao` — read/write the floor via the `PreferenceSource` extensions from §1.6.**

  **Constructor injection:** `SearchEngine`, `RateLimitedApiQueue`, `NewReleasesDao`, `FavoriteDao`, `PreferenceSource`, `Clock` (kotlinx-datetime), and a `CoroutineDispatcher` (IO).

  **State model:** a single `MutableStateFlow<WorkState>` (the only mutable state in the class) plus a `MutableSharedFlow<BrainzApiError>` for `errors`. Expose `workState: StateFlow<WorkState>` and `errors: Flow<BrainzApiError>` (read-only views).

  **Public state Flow (composed, not stored):** expose a single `observeState(): Flow<NewReleasesState>` that combines the three reactive sources so the UI binds to one stream:
  ```kotlin
  fun observeState(): Flow<NewReleasesState> = combine(
      workState,
      newReleasesDao.observeNewReleases(),
      errors.onStart { emit(emptyList()) },
  ) { work, releases, errs -> NewReleasesState(work, releases, errs) }
  ```
  `errors.onStart { emit(emptyList()) }` is used so the first emission arrives even when no error has been reported yet — the consumer never sees an empty-state UI flash caused by waiting for the first error.

  **Read step (`syncNewReleases`) — `combine` over the two inputs:** the sync floor and the favorite-artist ids are both reactive sources that can change while the app is alive. Use `combine` to derive both from their canonical flows in a single expression, then `first()` for a one-shot read:
  ```kotlin
  private val (dateFloor, favoriteIds) = combine(
      preferenceSource.getAppSettings(),
      favoriteDao.getFavoriteArtistsIds(),
  ) { settings, ids ->
      val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
      val floor = settings.newReleasesLastOpenedDay
          ?.plus(1, DateTimeUnit.DAY)
          ?: today.minus(30, DateTimeUnit.DAY)
      floor to ids
  }.first()
  ```
  The whole "read settings → compute floor → read favorites" block becomes one expression. `combine` makes the dependency explicit: "the floor is a pure function of (settings, today)". `first()` is correct because the sync is page-open triggered, not reactive to favorite changes.

  **Empty case — early return with `takeUnless`:** no API call, no DB write:
  ```kotlin
  favoriteIds.takeUnless(List<String>::isNotEmpty)?.let {
      workState.value = WorkState.IDLE
      return Result.success(Unit)
  }
  ```

  **Batched sync — pure `fold` over a list:** chunk into 50-id batches and `fold` over them with a `Pair<WorkState, Boolean>` accumulator (`workState, partialSync`). The fold is pure — no side effects inside; only the final assignment is a side effect. `Result.fold` translates each batch's outcome into a new accumulator value:
  ```kotlin
  val (finalState, partial) = favoriteIds
      .chunked(BATCH_SIZE)
      .fold(WorkState.LOADING to false) { (_, partial), batch ->
          currentCoroutineContext().ensureActive()
          fetchAndPersistBatch(batch, dateFloor, today)
              .fold(
                  onSuccess = { workState.value to partial },
                  onFailure = { error ->
                      _errors.tryEmit(error.toBrainzApiError())
                      WorkState.PARTIAL_SYNC to true
                  },
              )
      }
  ```
  Note: `workState.value` is read at the start (to avoid stale accumulator state) and the pair is re-emitted each fold. The single side effect — `workState.value =` — happens at the end.

  **`fetchAndPersistBatch` — `flatMapLatest` over offsets:** inside the batch helper, paginate at `limit=100` using a small `flow` builder with `emitAll`. Each page is a side-effecting upsert; the flow shape is `Flow<List<NewReleaseEntity>>` that emits one list per page. `currentCoroutineContext().ensureActive()` at the top of each page iteration handles cancellation:
  ```kotlin
  private suspend fun fetchAndPersistBatch(
      batch: List<String>,
      floor: LocalDate,
      today: LocalDate,
  ): Result<Unit> = runCatching {
      val query = buildReleaseGroupQuery(floor, today, batch)
      flow {
          var offset = 0
          while (offset < MAX_PAGES * PAGE_SIZE) {
              currentCoroutineContext().ensureActive()
              val page = rateLimitedApiQueue.enqueue { searchEngine.searchReleaseGroups(query, offset, PAGE_SIZE) }
                  .getOrThrow()
              if (page.releaseGroups.isEmpty()) break
              newReleasesDao.upsertNewReleases(page.releaseGroups.toEntities())
              offset += PAGE_SIZE
              if (offset >= page.count) break
          }
      }.collect()
  }
  ```

  **Completion — `also` for the state write, `let` for the side-effecting write:** derive the post-state from the accumulator, assign it, and conditionally write the floor. The two side effects are separated so neither hides the other:
  ```kotlin
  val postState = if (partial) WorkState.PARTIAL_SYNC else WorkState.IDLE
  workState.also { it.value = postState }
  if (!partial) preferenceSource.setNewReleasesFloor(today, Clock.System.now())
  return Result.success(postState)
  ```

  **Cancellation:** prefer `currentCoroutineContext().ensureActive()` at the start of each chunk fold and each page iteration, rather than wrapping the whole body in a `try/catch(CancellationException)`. The structured concurrency of the caller's scope is the cancellation mechanism.

  **State transitions only at boundaries:** the only places `workState` is assigned are the entry (`LOADING`), the empty-case early return, and the final assignment. No `workState.value = X` inside the fold body. The fold is pure in `(state, partial)` until the final assignment.
- [x] 5.3 Register `NewReleasesRepository` as a Koin `@Single` in `appModule.kt` (binding interface to `ApiNewReleasesRepository`).

## 6. Share MediaDetails factory

- [x] 6.1 Extract the `getMediaDetails` factory from `FavoritesHost` and `ArtistsHost` into a top-level function `screens/mainScreen/pages/sharedPanels/domain/mediaDetailsPanel/getMediaDetails.kt`:
  ```kotlin
  fun getMediaDetails(
      config: MediaDetailsConfig,
      context: ComponentContext,
  ): MediaDetails
  ```
  The factory uses the same Koin lookup pattern as the existing in-host versions.
- [x] 6.2 Update `FavoritesHost` and `ArtistsHost` to use the shared `::getMediaDetails` in their `childPanels(extraFactory = ...)` call. Remove the local copy of the factory from each host.

## 7. Tests

- [x] 7.1 `RateLimitedApiQueueTest` (jvmTest): verify two enqueued blocks execute ≥ 1s apart; verify cancellation mid-queue stops further work; verify exception in one block is propagated to the caller without poisoning the queue.
- [x] 7.2 `NewReleasesRepositoryTest` (jvmTest): use a `FakeSearchEngine` that returns a queue of `SearchReleaseGroupResponse` per `searchReleaseGroups` call, and a `FakePreferenceSource` (or a real `PreferenceSource` backed by a temporary `PreferenceDataStoreFactory.createWithPath { tmpFile.absolutePath }` in a JVM temp dir) that exposes the same `getAppSettings` / `setNewReleasesFloor` extension surface.
  - 7.2.a: User with 30 favorites and a single-page response → 1 API call, 30 rows in `new_releases` with `state = UNSEEN`, `workState = IDLE`, settings `newReleasesLastOpenedDay = today`, settings `newReleasesLastSyncCompletedAt` is recent.
  - 7.2.b: User with 500 favorites → 10 API calls, all 500 artist-ids spanned, settings `newReleasesLastOpenedDay = today`.
  - 7.2.c: User with 30 favorites and a 250-result response → 3 API calls within one batch (offsets 0, 100, 200), all 250 rows persisted.
  - 7.2.d: Pre-existing `SEEN` row in DB is re-fetched → its state remains `SEEN` (not reset to `UNSEEN`).
  - 7.2.e: One batch returns an error → subsequent batches are not issued, `workState = PARTIAL_SYNC`, settings `newReleasesLastOpenedDay` is NOT updated, settings `newReleasesLastSyncCompletedAt` is NOT updated.
  - 7.2.f: Cancellation mid-sync → no further batches issued; previously-completed batches remain in DB; settings are NOT updated.
  - 7.2.g: User with 0 favorites → no API calls, empty result, no DB writes, settings unchanged.
  - 7.2.h: First sync ever (settings `newReleasesLastOpenedDay = null`) → floor is `today - 30 days` (assert by capturing the query string in the fake).
  - 7.2.i: Subsequent sync (settings `newReleasesLastOpenedDay = D`) → floor is `D + 1 day`.
  - 7.2.j: Settings are read from DataStore before each sync, not from an in-memory cache — close and re-open the `DataStore<Preferences>` and confirm the next sync uses the persisted floor.
- [x] 7.3 `ApiReleasesRepositoryTest` (existing) → if it exists, ensure it still passes after the rate-limited queue retrofit. If not, add a smoke test that 5 paginated calls take ≥ 4s (4 delays between 5 calls).
- [x] 7.4 `PreferenceSourceAppSettingsTest` (jvmTest): verify that `getAppSettings()` returns the right `AppSettings` for empty DataStore (all-null), for a floor-only write (timestamp still null), for a timestamp-only write (floor still null), and that `setNewReleasesFloor(today, now)` is atomic (both keys visible after the suspending call returns). Use a temporary `.preferences_pb` file in a JVM temp dir, mirroring the existing DataStore pattern.

## 8. NewReleasesHost (UI shell)

- [x] 8.1 Add `NewReleasesHost` to `screens/mainScreen/pages/newReleases/domain/pageHost/NewReleasesHost.kt`. Implements `PagerComponent` and `Page` interfaces. Uses Decompose `childPanels(extra = ::getMediaDetails)` (the shared factory). `lifecycle.doOnStart` triggers `viewModel.syncNewReleases()`.
- [x] 8.2 Add `NewReleasesList` ViewModel to `screens/mainScreen/pages/newReleases/domain/list/NewReleasesList.kt`:
  - Wraps `repository.getNewReleases()` in `Pager(apiConfig()) { ... }.flow.cachedIn(viewModelScope)`.
  - Applies `groupedBy { it.artistName }`.
  - Exposes a `loadFromRemote` action that calls `repository.syncNewReleases()`.
  - Surfaces `workState` and `errors` from the repository.
- [x] 8.3 Add `NewReleasesList` Composable to `screens/mainScreen/pages/newReleases/ui/list/NewReleasesList.kt`:
  - Uses `HandlePagingItems` to drive Loading/Empty/Error/Content states.
  - Renders `LazyPagingItems<GroupedItem<NewReleaseEntity, String>>` via the existing `ArtistReleasesList` (with the artist-name grouping key).
  - Each row uses the existing `ReleaseListItem`, extended with a `unseen: Boolean` parameter that draws the dot indicator and bolds the title. The "open full cover" action remains a no-op for the New Releases list (cover URL may be null; placeholder is fine for v1).
  - On `SelectRelease`, calls `openMedia(releaseId, releaseName)` — same signature as the existing per-artist releases panel.
  - On swipe-to-dismiss, calls `viewModel.dismiss(releaseId)`.
- [x] 8.4 Add `NewReleasesListAction` to `screens/mainScreen/pages/newReleases/domain/list/NewReleasesListAction.kt`:
  - `SelectRelease(releaseId: String, releaseName: String)`
  - `Dismiss(releaseId: String)`
  - `LoadFromRemote`
  - `OnMediaOpened(releaseId: String)` — fired when the user opens the media details panel, triggers `viewModel.markSeen(releaseId)`.
- [x] 8.5 Add the `onErrorAction` slot to surface a partial-sync snackbar when `workState == PARTIAL_SYNC`. Mirror the existing `FavoritesHost` snackbar pattern.
- [x] 8.6 Wire the `lifecycle.doOnStart` auto-sync.

## 9. Pager integration

- [x] 9.1 In `MainPagerComponent.kt:60`, change `PagerConfig.Releases -> DummyPage` to `PagerConfig.Releases -> NewReleasesHost(...)`. Construct the host with the same Koin lookup pattern as `FavoritesHost` and `ArtistsHost`.
- [x] 9.2 Delete the `DummyPage` object (lines 81–85) and its `//TODO` comment. (No other code references it.)
- [x] 9.3 Replace the throwaway `screens/mainScreen/pages/newReleases/ui/SearchPage.kt` with the real `NewReleasesHost` UI.

## 10. Build and verify

- [ ] 10.1 Run `./gradlew :composeApp:assembleDebug` to regenerate KSP for the new entity, dao, and the database bump. Confirm clean build.
- [ ] 10.2 Run `./gradlew :composeApp:jvmTest` and confirm all tests pass (including the new ones from §7).
- [ ] 10.3 Manual smoke test on `:androidApp` debug build:
  - Add 5–10 favorite artists.
  - Open the New Releases page → verify a sync runs, results stream in, dots appear on unseen releases.
  - Tap a release → verify media details panel opens, dot disappears on return.
  - Swipe a release to dismiss → verify it disappears.
  - Close the app and re-open the New Releases page → verify a fresh sync runs and no previously-seen releases re-appear with dots.
  - Force-stop the app between the first sync and the second sync → verify the second sync re-fetches from the persisted `newReleasesLastOpenedDay` (settings survive the app kill because they are in DataStore, not Room).
  - Verify `ApiReleasesRepository.syncReleases` for a single artist with > 50 release-groups now takes ≥ N seconds (where N = page count - 1), confirming the rate-limit retrofit.
