# AGENTS.md — FollowMyMus

Kotlin Multiplatform (KMP) app targeting Android, iOS, Desktop (JVM).

## Modules

| Module | Role |
|---|---|
| `:composeApp` | Shared library — all common UI, domain, data layer |
| `:androidApp` | Thin Android wrapper, depends on `:composeApp` |
| `iosApp/` | iOS Xcode project (Swift entry point) |

## Key Commands

```shell
# Android debug build
./gradlew :androidApp:assembleDebug

# Desktop run (JVM)
./gradlew :composeApp:run

# Run all tests
./gradlew :composeApp:allTests

# Run JVM tests only
./gradlew :composeApp:jvmTest

# Clean + rebuild (needed after KSP / BuildKonfig changes)
./gradlew clean :composeApp:assembleDebug
```

iOS: open `iosApp/` in Xcode and run from there.

## Setup Requirements

- **JDK 21** — enforced by `jvmToolchain(21)` in `composeApp/build.gradle.kts`
- **`local.properties`** at project root — required by BuildKonfig plugin:
  ```
  projectId=<supabase-project-id>
  publishableKey=<supabase-publishable-key>
  secretKey=<supabase-secret-key>
  ```
  Without these the build fails. Values are Supabase credentials.

## Architecture

- **Navigation**: Decompose (`RootComponent` → `MainRootComponent` → pager components)
- **DI**: Koin with `@Module` / `@Single` / `@Factory` annotations + KSP codegen
  - `FollowMyMusApp` is the `@KoinApplication` entry point (modules: `AppModule`, `DbModule`)
  - Platform-specific DI in `appModule.{android,ios,jvm}.kt`
- **HTTP**: Ktor client (shared in `AppModule.provideMusicBrainzClient()`)
- **Backend**: Supabase (auth, realtime, postgrest) — client in `AppModule.provideSupabaseClient()`
- **Local DB**: Room with KSP compiler, schemas in `composeApp/schemas/`
- **Analytics**: Kotzilla SDK (custom Maven repo at `repository.kotzilla.io`)
- **Package**: `io.github.alexmaryin.followmymus`

### Source Set Layout

```
composeApp/src/
  commonMain/   ← shared code (UI, domain, data, DI)
  androidMain/  ← Android-specific (CameraX, ML Kit, Koin Android, OkHttp)
  iosMain/      ← iOS-specific (Darwin Ktor client)
  jvmMain/      ← Desktop-specific (Swing coroutines, ZXing QR)
  commonTest/   ← shared tests
  jvmTest/      ← JVM tests (kotlin.test, MockK, Koin test, kotlinx-coroutines-test)
```

### Paging Library (`core/paging/`, `core/ui/pagingHandler.kt`)

Domain-agnostic paging utilities shared by Search, Favorites, Releases, and Media screens.

- **`PagingDefaults`** — single source of truth for page sizes:
  - `API_PAGE = 50`, `API_INITIAL = 50` — for `Pager(apiConfig())` over the network
  - `ROOM_PAGE = 20`, `ROOM_INITIAL = 40`, `PREFETCH = 20`, `MAX_SIZE = 100` — for `Pager(roomConfig())` over Room
  - `MAX_RELEASE_PAGES = 200`, `MAX_MEDIA_PAGES = 200` — safety caps for all-pages network sync (overridable in tests via the `var` form)
  - Repositories MUST use `apiConfig()` / `roomConfig()` rather than building `PagingConfig` literals inline
- **`PagingError`** — sealed type (`Network`, `Server`, `InvalidResponse`, `Unknown`) with `defaultPagingError(Throwable)` factory
- **`PagingDataExt`** — `RoomPagingCount` and `NetworkPagingCount` helpers; `.totalCount(): Flow<Int?>` reads a side-channel updated by the source
- **`PagingDataGrouping`** — `PagingData<T>.groupedBy<K>(keySelector)` extension via `insertSeparators`; produces `GroupedItem<T, K>` (Header / Item)
- **`HandlePagingItems`** (in `core/ui/pagingHandler.kt`) — Compose wrapper that computes a single `PagingUiState<T>` (`Loading` / `Empty` / `Error` / `Content`) per recomposition and dispatches to a single mutually-exclusive branch (`OnLoading` / `OnEmpty` / `OnError` / `OnContent`). Callers pass `errorMapper` (defaults to `::defaultPagingError`) and optional `onErrorAction` / `onAppendError` / `onAppendLoading` slots. Refresh-fails-on-populated-list keeps the data and surfaces the error via `onErrorAction` exactly once per error transition.

### All-Pages Network Sync Pattern

`ApiReleasesRepository.syncReleases(artistId)` and `ApiMediaRepository.fetchReleasesMedia(releaseId)` both follow the same shape:

1. Reset `WorkState` to `LOADING` and clear `totalNetworkReleases` / `totalNetworkMedia` (`MutableStateFlow<Int?>(null)`).
2. Loop `searchEngine.search*(id, offset, limit = API_PAGE)` from `offset = 0`:
   - First successful page sets `totalCount = response.count.coerceAtLeast(releases.size)` and publishes it to the corresponding `totalNetwork*` flow.
   - Each page is persisted in its own Room `@Transaction` call, then cover art is fetched in parallel (`flatMapMerge(25)` for releases, `flatMapMerge(50)` for media) before the next request.
   - `currentCoroutineContext().ensureActive()` at the top of each iteration — cancellation at page N keeps pages 0..N-1 in Room.
   - On per-page `Result.Error`: stop the loop, send to `_errors`, mark `partialSync = true` only if `offset > 0` (a first-page error is not a partial sync).
3. After the loop, if `offset < totalCount` (cap hit or empty trailing page) set `partialSync = true`.
4. Final `workState`: `PARTIAL_SYNC` if any partial-sync condition fired, else `IDLE`.

`WorkState` enum: `IDLE`, `LOADING`, `PARTIAL_SYNC`. UI consumers (ReleasesList, MediaDetails) emit a snackbar on `PARTIAL_SYNC` via `BrainzApiError.getPartialSyncMessage()`.

## KSP Codegen

Koin annotations and Room both use KSP. Generated common metadata goes to:
`composeApp/build/generated/ksp/metadata/commonMain/kotlin`

If you add a new `@Module`, `@Single`, `@Factory`, or `@Component` annotation, run a build to regenerate. The `ksp*` tasks depend on `kspCommonMainKotlinMetadata`.

## Tech Stack (versions)

| Library | Version |
|---|---|
| Kotlin | 2.3.10 |
| Compose Multiplatform | 1.11.0-alpha04 |
| AGP | 8.12.3 |
| Koin | 4.1.1 |
| KSP | 2.3.0 |
| Supabase BOM | 3.4.1 |
| Ktor | 3.4.1 |
| Decompose | 3.4.0 |
| Room | 2.8.4 |
| Coil | 3.4.0 |
| kotlinx-coroutines | 1.10.2 |
| MockK | 1.14.9 |

## Supabase Breaking Change — Data API Exposure (Oct 30, 2026)

Starting **October 30, 2026**, Supabase will no longer auto-expose new tables in the `public` schema to the Data API (PostgREST). Explicit `GRANT` statements will be required.

### Impact on this project

- **Table**: `favorite_artists` (used via `supabase.from()` in `DefaultSupabaseDb.kt`)
- **Existing table**: keeps current grants, stays reachable
- **New tables**: will NOT be reachable via Data API without explicit `GRANT`

### Required action for any new table in `public`

```sql
grant select, insert, update, delete on public.your_table to anon, authenticated, service_role;
alter table public.your_table enable row level security;
create policy "your_policy" on public.your_table for all to authenticated using (true);
```

See `docs/issues/supabase-change.md` for full details.

## OpenSpec

This project uses [OpenSpec](https://github.com/Fission-AI/OpenSpec) for spec-driven change management.

- `openspec/specs/` — main capability specs (one file per capability: `releases`, `media`, `paging`, etc.)
- `openspec/changes/` — active changes (proposal, design, specs delta, tasks)
- `openspec/changes/archive/` — archived changes (one per day: `YYYY-MM-DD-<name>/`)
- Skills: `/opsx-propose`, `/opsx-apply`, `/opsx-archive`, `/opsx-explore`, `/opsx-sync-specs`

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, invoke the `skill` tool with `skill: "graphify"` before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
