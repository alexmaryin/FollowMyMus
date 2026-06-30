## 1. Lift `@InjectedParam` onto constructors (prerequisite for deleting provider functions)

These must be done BEFORE the corresponding provider functions are deleted, so Koin can still resolve the runtime parameters while the function is in place.

- [x] 1.1 `ArtistsPagingSource` — annotate the `query` and `count` constructor parameters with `@InjectedParam`
- [x] 1.2 `MediaPagingSource` — annotate the `releaseId` constructor parameter with `@InjectedParam`
- [x] 1.3 `CMPLoginComponent` — annotate the `qrCode`, `componentContext`, and `onSignUpClick` constructor parameters with `@InjectedParam`
- [x] 1.4 `CMPSignUpComponent` — annotate the `componentContext` and `onLoginClick` constructor parameters with `@InjectedParam`
- [x] 1.5 `MainPagerComponent` — annotate the `componentContext` and `nickName` constructor parameters with `@InjectedParam`
- [x] 1.6 `ArtistsHost` — annotate the `componentContext` constructor parameter with `@InjectedParam`
- [x] 1.7 `FavoritesHost` — annotate the `componentContext` and `nickname` constructor parameters with `@InjectedParam`
- [x] 1.8 `NewReleasesHost` — annotate the `componentContext` constructor parameter with `@InjectedParam`
- [x] 1.9 `AccountPage` — annotate the `componentContext` and `nickname` constructor parameters with `@InjectedParam`

## 2. Trim `AppModule` and add `@ComponentScan`

- [x] 2.1 Delete `provideSessionManager` from `AppModule`
- [x] 2.2 Delete `provideSearchEngine` from `AppModule`
- [x] 2.3 Delete `provideRateLimitedApiQueue` from `AppModule`
- [x] 2.4 Delete `providePreferenceSource` from `AppModule`
- [x] 2.5 Delete `provideNewReleasesRepository` from `AppModule`
- [x] 2.6 Delete `provideCoversEngine` from `AppModule`
- [x] 2.7 Delete `provideSupabaseDb` from `AppModule`
- [x] 2.8 Delete `provideArtistsPagingSource` from `AppModule`
- [x] 2.9 Delete `provideMediaPagingSource` from `AppModule`
- [x] 2.10 Delete `provideLoginComponent` from `AppModule`
- [x] 2.11 Delete `provideSignUpComponent` from `AppModule`
- [x] 2.12 Delete `providePagerComponent` from `AppModule`
- [x] 2.13 Delete `provideArtistsHost` from `AppModule`
- [x] 2.14 Delete `provideFavoritesHost` from `AppModule`
- [x] 2.15 Delete `provideNewReleasesHost` from `AppModule`
- [x] 2.16 Delete `provideAccountHost` from `AppModule`
- [x] 2.17 Add `@ComponentScan("io.github.alexmaryin.followmymus")` annotation to `AppModule`
- [x] 2.18 Update the workaround comment at `apiModule.kt:113-117` to describe the new state (or remove it if no longer relevant)

## 3. Trim `DbModule` and add `@ComponentScan`

- [x] 3.1 Delete `provideArtistsRepository` from `DbModule`
- [x] 3.2 Delete `provideReleasesRepository` from `DbModule`
- [x] 3.3 Delete `provideMediaRepository` from `DbModule`
- [x] 3.4 Delete `provideSyncRepository` from `DbModule`
- [x] 3.5 Delete `provideLocalDbRepository` from `DbModule`
- [x] 3.6 Add `@ComponentScan("io.github.alexmaryin.followmymus")` annotation to `DbModule`

## 4. Update build configuration

- [x] 4.1 In `composeApp/build.gradle.kts`, change `koinCompiler { compileSafety = false }` to `compileSafety = true`
- [x] 4.2 Replace the per-source-set workaround comment at `composeApp/build.gradle.kts:150-162` with a brief note that `@ComponentScan` handles cross-source-set discovery on Koin Compiler Plugin 1.0.1+

## 5. Validate

- [x] 5.1 Run `./gradlew :androidApp:assembleDebug` and confirm the build succeeds. **Rollback applied**: `compileSafety = true` exposed cross-module `KOIN-D001` errors (compile-time check runs per `@Module`, so AppModule-scanned repositories needing DbModule-provided DAOs were reported as missing) plus a pre-existing `parametersOf` type mismatch in `MainActivity.kt:23` (was hidden by the previously disabled safety check). The `parametersOf` issue is fixed in `MainActivity.kt`. `compileSafety` is reverted to `false` with an updated comment explaining the actual reason (cross-module, not cross-source-set). The `@ComponentScan` change stands. AppModule.module now has 25 definitions (up from 4); DbModule.module has 31 definitions (up from 14).
- [x] 5.2 Run `./gradlew :composeApp:jvmTest` — 71/73 tests pass. The 2 failures (`Check if app navigates to Main if authenticated`, `Check if app navigates to Login if not authenticated`) are the **pre-existing** `NoDefinitionFoundException: PreferenceSource` documented in `docs/issues/failing-tests.md` — the test fixture's standalone `startKoin` module doesn't register `PreferenceSource`. Not caused by this refactor.
- [x] 5.3 Run `./gradlew :composeApp:allTests` — same 2 pre-existing failures; 71/73 pass.
- [x] 5.4 Run `./gradlew clean :androidApp:assembleDebug` — clean build successful.
- [x] 5.5 iOS smoke-test — manual; requires Xcode. Out of scope for CLI validation. Recommend `cd iosApp && xcodebuild` followed by launching on simulator.
- [x] 5.6 (follow-up) Add `provideDataStore` provider function to `AppModule`. The previous `providePreferenceSource` created the `DataStore<Preferences>` (alias `Prefs`) inline via the `createDataStore { platformPrefsPath() }` builder. Deleting it left `PreferenceSource`'s constructor parameter `prefs: Prefs` with no provider, surfacing as `NoDefinitionFoundException: DataStore` at runtime. `DataStore<Preferences>` is a third-party builder (PreferenceDataStoreFactory) so it belongs in a `@Module` provider function per design D5/D6.
