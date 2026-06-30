## Context

The project is a Kotlin Multiplatform app (Android, iOS, JVM Desktop) using Koin 4.2.2 with the Koin Compiler Plugin 1.0.1 (not the deprecated KSP processor). DI entry point: `@KoinApplication(modules = [AppModule::class, DbModule::class]) object FollowMyMusApp` referenced by `startKoin<FollowMyMusApp>` in `FollowMyMusAndroid.kt` (androidMain), `StartHelper.kt` (iosMain), and `main.kt` (jvmMain).

21 production classes already carry Koin class-level annotations (`@Single` / `@Factory` with optional `binds = [...]`). `AppModule` and `DbModule` nevertheless re-register each of those classes through a provider function inside the `@Module class`. The duplication originated as a workaround for a now-likely-obsolete KMP `@ComponentScan` gap. The Koin docs treat `@ComponentScan("com.foo")` as the standard way to auto-discover annotated classes in a KMP project: it traverses across Gradle modules for the same package.

The Koin compiler plugin currently has `compileSafety = false` (see `composeApp/build.gradle.kts:150-162`) with a per-source-set comment that explains the gap as a runtime-only validation choice.

## Goals / Non-Goals

**Goals:**

- Let `@ComponentScan` discover the 21 already-annotated classes from the project's root package so that the redundant provider functions in `AppModule` and `DbModule` can be deleted.
- Document a project convention: definitions live as Koin annotations on the owning class; `@Module` classes contain only external-library provider functions and bean properties; `@ComponentScan` handles auto-discovery.
- Re-enable `compileSafety = true` if the historical KMP gap is no longer present in Koin Compiler Plugin 1.0.1.
- Keep the runtime DI graph identical: same definitions, scopes, qualifiers, parameter injections.

**Non-Goals:**

- Rewriting any third-party builder DSL (Ktor, Supabase, Room) into something Koin could "own" — those remain provider functions in the `@Module` class.
- Splitting `AppModule` / `DbModule` into more granular modules — keep the current two-module shape.
- Refactoring the KMP `expect fun getHttpEngine()` / `getDbMusicBrainzDbFactory()` per-platform wiring — that pattern is already idiomatic and the docs explicitly note the only reason to switch to expect/actual `@Module class` would be platform-specific builders, which is not our case.
- Modifying the test fixtures in `jvmTest` (they use the classic Koin DSL standalone; the refactor leaves them untouched).
- Touching the iOS / Android / JVM `startKoin<FollowMyMusApp>` call sites — the entry point is unchanged.

## Decisions

### D1. Use `@ComponentScan("io.github.alexmaryin.followmymus")` on each `@Module` (Path B from exploration)

**Rationale:** The Koin docs treat this as the standard pattern. The root package covers all annotated classes in `commonMain`, `androidMain`, `iosMain`, and `jvmMain` — they all live under `io.github.alexmaryin.followmymus`. The Koin Compiler Plugin 1.0.1 release notes address KMP source-set concerns.

**Alternatives considered:**

- *Path A — conservative*: trim modules without `@ComponentScan`. Smaller diff, but the project keeps the workaround comment and the @Module classes still need explicit class-list maintenance. Doesn't follow the docs' recommended pattern.
- *Path C — `@Module(includes = [...])`*: would let us compose modules. The project only has two modules, so this adds complexity without payoff. Skipped.

### D2. Add `@ComponentScan` to BOTH `AppModule` AND `DbModule`

**Rationale:** Both modules already exist and own the redundant provider functions. Splitting by area (DI / persistence) keeps the natural separation. The package string is identical because both modules scan the same root package.

**Alternatives considered:**

- *Single shared `@Module @ComponentScan`*: would force the two-module convention to collapse. Loses the DI / persistence separation. Skipped.
- *Use the default-package shorthand (`@ComponentScan` with no argument)*: not supported per the Koin docs — the package string is required.

### D3. Lift `@InjectedParam` from provider functions to constructor parameters (no separate "annotation on provider function")

**Rationale:** The Koin docs explicitly support `@InjectedParam` on constructor parameters of annotated classes. The current provider functions carry `@InjectedParam` because the parameter wasn't annotated at the constructor. Moving the annotation to the constructor is a behavior-preserving lift.

**Affected classes (9):** `ArtistsPagingSource` (query, count), `MediaPagingSource` (releaseId), `CMPLoginComponent` (qrCode, componentContext, onSignUpClick), `CMPSignUpComponent` (componentContext, onLoginClick), `MainPagerComponent` (componentContext, nickName), `ArtistsHost` (componentContext), `FavoritesHost` (componentContext, nickname), `NewReleasesHost` (componentContext), `AccountPage` (componentContext, nickname).

**Alternative considered:** Keep `@InjectedParam` only on the provider function. This would require keeping the provider functions and thus defeating the cleanup. Skipped.

### D4. Re-enable `compileSafety = true` with a documented fallback

**Rationale:** The historical comment (`build.gradle.kts:150-162`) describes a per-source-set gap that may have been fixed in Koin Compiler Plugin 1.0.1. Re-enabling is the right default; if the gap is still present, the rollback is a one-line config change.

**Approach:** Land the change with `compileSafety = true` as the first attempt. If `:composeApp:assembleDebug` fails with the A4 call-site complaint, set it back to `false` and document the rollback in the change's notes. The `@ComponentScan` change stands either way.

**Alternative considered:** Leave `compileSafety = false` permanently. The comment in the file would still be misleading, and we'd be giving up a real correctness check. Skipped.

### D5. Keep `provideAuth(supabase) = supabase.auth` and `provideSessionTransferChannel(...)` as provider functions

**Rationale:**

- `Auth` is a property of the `SupabaseClient` bean (`supabase.auth`), not a class Koin can annotate. Provider function is the right tool.
- `RealtimeChannel` is a runtime-parameter-bound factory — its dependency on `@InjectedParam transferId` only makes sense in a function with `transferId` in scope. Annotating the function (not the class) is the right tool here.

**Alternative considered:** Wrap each as a class with a `create(...)` method. Adds a class for no behavioral benefit. Skipped.

### D6. Keep the 9 Room DAO providers in `DbModule`

**Rationale:** DAOs are properties of the generated Room database (`database.artistDao()`, etc.). They are not ownable types — `MusicBrainzDatabase` is created by the Room `Database.Builder<MusicBrainzDatabase>` DSL, which is a provider function. The DAOs ride along on the database bean.

**Alternative considered:** Generate a Koin module from Room. Out of scope; would require a custom Room schema validator. Skipped.

## Risks / Trade-offs

- **[R1] `@ComponentScan` may still not cross source sets in KMP on Koin Compiler Plugin 1.0.1** → Mitigation: validate via `./gradlew :composeApp:assembleDebug`. If the generated `AppModule.module` does not contain the scanned definitions, fall back to explicit `@Module(includes = [...])` or keep `compileSafety = false` and ship the cleanup anyway. Document the gap in a follow-up issue if the rollback is needed.
- **[R2] `@InjectedParam` lift changes the primary constructor signature of 9 classes** → Mitigation: each lifted parameter is already `private val` in the constructor; the public API is unchanged. No caller of `ArtistsPagingSource(...)` etc. should exist outside Koin (these are factory-bound). The new-releases spec is unaffected because it does not depend on constructor signatures.
- **[R3] Re-enabling `compileSafety = true` may surface unrelated "missing definition" warnings** → Mitigation: address each as it appears; if a warning is a known false positive for KMP, document and disable. Do not blanket-revert `compileSafety`.
- **[R4] Test fixture drift in `RootTest` (the `NoDefinitionFoundException: PreferenceSource` issue) is now fixable** → Mitigation: the change does not fix this directly (tests use standalone `startKoin`), but the new `AppModule` will be importable as a single module from the test fixture. A follow-up to `docs/issues/failing-tests.md` is appropriate but out of scope here.
- **[R5] Stale comments in `apiModule.kt:113-117` and `build.gradle.kts:150-162`** → Mitigation: refresh both as part of the change so the file state matches reality.

## Migration Plan

1. Land in a single PR. The runtime behavior is identical; the risk is at build time only.
2. Rollback: revert the commit. The redundant provider functions and `compileSafety = false` are recoverable from the prior state.

## Open Questions

- None blocking. The decision matrix is settled.
