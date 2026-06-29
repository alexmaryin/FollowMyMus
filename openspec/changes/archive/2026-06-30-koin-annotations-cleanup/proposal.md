## Why

The project has been partially migrated to the Koin 4.2 annotations API: 21 of the production classes already carry `@Single` / `@Factory` annotations, but `AppModule` and `DbModule` still re-register each of those classes through a redundant provider function inside the `@Module class`. The duplication is a known workaround for what was, on an older Koin compiler plugin, a `@ComponentScan`-does-not-cross-source-sets problem (see `apiModule.kt:113-117` and `composeApp/build.gradle.kts:150-162`). The project is now on Koin 4.2.2 with the Koin Compiler Plugin 1.0.1, and the docs treat `@ComponentScan("com.foo")` as the standard way to auto-discover annotated classes in a KMP project. It's time to drop the workaround and let `@ComponentScan` do its job.

## What Changes

- Add `@ComponentScan("io.github.alexmaryin.followmymus")` to `AppModule` and `DbModule` so the 21 already-annotated classes are auto-discovered.
- Delete the 16 redundant provider functions from `AppModule` that wrap classes which already have a class-level Koin annotation.
- Delete the 5 redundant provider functions from `DbModule` (the repositories and `RoomRepository`).
- Lift `@InjectedParam` from the 8 paging-source and host-component provider functions onto the corresponding constructor parameters of `ArtistsPagingSource`, `MediaPagingSource`, `CMPLoginComponent`, `CMPSignUpComponent`, `MainPagerComponent`, `ArtistsHost`, `FavoritesHost`, `NewReleasesHost`, `AccountPage`.
- Re-enable `koinCompiler { compileSafety = true }` in `composeApp/build.gradle.kts` and remove the per-source-set workaround comment.
- Update the `@ComponentScan`-doesn't-work comment in `apiModule.kt:113-117` to describe the new state.
- Keep the 4 legitimate provider functions in `AppModule` (Ktor `HttpClient`, Supabase client, `Auth` property, `RealtimeChannel` factory) and the 10 legitimate provider functions in `DbModule` (Room `Database.Builder<MusicBrainzDatabase>` and the 9 Room DAO accessors) — these are external-library builders and bean properties, not ownable types.

No user-visible behavior change. The Koin graph is identical at runtime.

## Capabilities

### New Capabilities

- `koin-annotations`: The project's DI wiring convention — definitions live as Koin annotations on the owning class (`@Single` / `@Factory` with optional `binds = [...]`), `@Module` classes contain only external-library provider functions and bean properties, and `@ComponentScan("io.github.alexmaryin.followmymus")` on each `@Module` handles auto-discovery.

### Modified Capabilities

None. `new-releases` and any other feature spec is unaffected — the runtime DI graph is identical.

## Impact

- **Files touched**:
  - `composeApp/src/commonMain/kotlin/.../di/apiModule.kt` — remove 16 redundant functions, add `@ComponentScan`
  - `composeApp/src/commonMain/kotlin/.../di/dbModule.kt` — remove 5 redundant functions, add `@ComponentScan`
  - `composeApp/src/commonMain/kotlin/.../paging/ArtistsPagingSource.kt` — `@InjectedParam` on constructor params
  - `composeApp/src/commonMain/kotlin/.../paging/MediaPagingSource.kt` — same
  - 6 host components (`ArtistsHost`, `FavoritesHost`, `NewReleasesHost`, `AccountPage`, `MainPagerComponent`, `CMPLoginComponent`, `CMPSignUpComponent`) — same
  - `composeApp/build.gradle.kts` — re-enable `compileSafety = true`, refresh the comment
- **Runtime**: zero change. Same definitions, same scopes, same qualifiers.
- **Build**: `compileSafety = true` is re-enabled; the Koin compiler plugin's A4 call-site check now runs per-source-set. If it still reports a false positive (the historical KMP issue), the change is non-blocking — re-disable `compileSafety` and ship `@ComponentScan` anyway. Document the rollback in the change's design.
- **Tests**: `jvmTest` uses standalone `startKoin { modules(module { ... }) }` and does not reference `AppModule` / `DbModule`. Unaffected.
- **Platform code**: `expect fun getHttpEngine()` / `expect fun getDbMusicBrainzDbFactory()` and their `actual` per-platform implementations are unchanged. The KMP expect/actual pattern is already idiomatic.
- **Dependencies**: none. This is a wiring cleanup using the same Koin 4.2.2 + koin-plugin 1.0.1 that is already on the classpath.
