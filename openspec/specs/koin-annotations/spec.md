# Koin Annotations Capability

## Purpose

The Koin DI graph in `composeApp` is declared using the Koin Compiler Plugin's class-level annotation API (Koin 4.2.2, `koin-plugin` 1.0.1). Every Koin-managed class carries its own `@Single` or `@Factory` annotation (with optional `binds = [...]` for interface→implementation mappings) on the class declaration; constructor parameters that vary per resolution carry `@InjectedParam` directly on the parameter. The two `@Module` classes — `AppModule` and `DbModule` — exist only for the cases Koin cannot annotate directly: third-party builder DSLs (Ktor `HttpClient`, Supabase client, Room `Database.Builder<T>`), properties of another bean, runtime-parameter-bound factories, and Room DAO property accessors. Both `@Module` classes carry `@ComponentScan("io.github.alexmaryin.followmymus")` so the compiler plugin auto-discovers every annotated class in the project root package across all KMP source sets (`commonMain`, `androidMain`, `iosMain`, `jvmMain`).

## Requirements

### Requirement: DI definitions are declared as Koin annotations on the owning class

The system MUST declare Koin definitions as class-level annotations (`@Single` or `@Factory`, with optional `binds = [...]` for interface→implementation mappings) on the class being registered. The constructor of that class MUST declare its Koin-injected dependencies as ordinary parameters; parameters that vary per resolution (e.g. PagingSource input, Decompose `ComponentContext`, callbacks) MUST be marked with `@InjectedParam` directly on the constructor parameter. Provider-function wrappers around such classes (a function inside a `@Module class` whose only purpose is to instantiate a class that already carries `@Single` / `@Factory`) are NOT allowed.

#### Scenario: Search engine is a single definition declared on the class

- **WHEN** the Koin graph is inspected at runtime via `module.verify()`
- **THEN** `SearchEngine` is registered as a `single` definition bound to `ApiSearchEngine`
- **AND** the registration source is the `@Single(binds = [SearchEngine::class])` annotation on `ApiSearchEngine`
- **AND** `AppModule` contains no `provideSearchEngine` function

#### Scenario: Repository is a single definition declared on the class

- **WHEN** the Koin graph is inspected at runtime via `module.verify()`
- **THEN** `ReleasesRepository` is registered as a `single` definition bound to `ApiReleasesRepository`
- **AND** the registration source is the `@Single(binds = [ReleasesRepository::class])` annotation on `ApiReleasesRepository`
- **AND** `DbModule` contains no `provideReleasesRepository` function

#### Scenario: Artists paging source uses `@InjectedParam` on its constructor

- **WHEN** `koinInject<ArtistsPagingSource>()` is called with `parametersOf("radiohead", networkCount)` from a search page
- **THEN** Koin instantiates `ArtistsPagingSource` with `query = "radiohead"` and `count = networkCount` bound to the constructor parameters marked `@InjectedParam`
- **AND** the search engine is resolved from the Koin graph (constructor parameter without `@InjectedParam`)

#### Scenario: Decompose host component is a factory declared on the class

- **WHEN** a `MainPagerComponent` is requested by the parent `RootComponent` with `parametersOf(componentContext, "alex")`
- **THEN** Koin instantiates `MainPagerComponent` with `componentContext` and `nickName` bound to the `@InjectedParam` constructor parameters
- **AND** `AppModule` contains no `providePagerComponent` function

### Requirement: `@Module` classes contain only external-library builder functions and bean properties

The system MUST use `@Module class X { ... }` only for the cases Koin cannot annotate directly: third-party builder DSLs (Ktor `HttpClient`, Supabase client, Room `Database.Builder<T>`), properties of another bean (e.g. `supabase.auth`), and runtime-parameter-bound factories (e.g. `RealtimeChannel` with a `transferId` parameter). Each provider function in a `@Module` class MUST return either a type Koin cannot annotate or a property of an already-registered bean. The system MUST NOT use `@Module` provider functions to instantiate a class that already has a class-level Koin annotation.

#### Scenario: Ktor HttpClient is a provider function

- **WHEN** the Koin graph is inspected
- **THEN** `HttpClient` is registered as a `single` definition from `AppModule.provideMusicBrainzClient()`
- **AND** the function body uses the Ktor DSL (`install(Logging)`, `install(ContentNegotiation)`, `install(HttpTimeout)`, `install(HttpRequestRetry)`)
- **AND** there is no `HttpClient` class with a class-level `@Single` annotation in the project

#### Scenario: Supabase Auth is a property provider

- **WHEN** the Koin graph is inspected
- **THEN** `Auth` is registered as a `single` definition from `AppModule.provideAuth(supabase)` whose body is `supabase.auth`
- **AND** the function exists because `Auth` is a property of `SupabaseClient`, not an ownable class

#### Scenario: RealtimeChannel is a runtime-parameter-bound factory

- **WHEN** `koinInject<RealtimeChannel> { parametersOf("transfer-id-123") }` is called
- **THEN** Koin calls `AppModule.provideSessionTransferChannel(supabase, transferId = "transfer-id-123")`
- **AND** the function exists because the channel's identity is per-transfer (runtime-bound) and not a stable bean

#### Scenario: Room DAOs are properties of the generated database

- **WHEN** the Koin graph is inspected
- **THEN** `ArtistDao`, `FavoriteDao`, `NewReleasesDao`, `MediaDao`, `ReleaseDao`, `ResourceDao`, `SyncDao`, `TransactionalDao`, and `RelationDao` are each registered as `single` definitions from `DbModule`
- **AND** each provider function body is `database.<dao>()` (a property accessor on the generated Room database)
- **AND** `MusicBrainzDatabase` itself is registered from `DbModule.provideMusicBrainzDatabase()` using the Room `Database.Builder<MusicBrainzDatabase>` DSL

### Requirement: `@ComponentScan` on each `@Module` auto-discovers annotated classes in the project root package

The system MUST annotate `AppModule` and `DbModule` with `@ComponentScan("io.github.alexmaryin.followmymus")` so the Koin Compiler Plugin auto-discovers the 21 annotated classes across all KMP source sets (`commonMain`, `androidMain`, `iosMain`, `jvmMain`). The `@ComponentScan` package MUST be the project's root package so that all annotated classes (current and future) are covered without further module changes. The Koin entry point `@KoinApplication(modules = [AppModule::class, DbModule::class])` MUST continue to reference the two `@Module` classes by reference.

#### Scenario: AppModule declares ComponentScan for the project root package

- **WHEN** the Koin compiler plugin generates `AppModule.module` at build time
- **THEN** the generated module contains the 16 annotated classes from the project's `io.github.alexmaryin.followmymus.*` package (e.g. `ApiSearchEngine`, `RateLimitedApiQueue`, `PreferenceSource`, `SupabaseSessionManager`, `ApiCoversEngine`, `ApiNewReleasesRepository`, `DefaultSupabaseDb`, `ApiReleasesRepository`, `ApiMediaRepository`, `ArtistsPagingSource`, `MediaPagingSource`, `CMPLoginComponent`, `CMPSignUpComponent`, `MainPagerComponent`, `ArtistsHost`, `FavoritesHost`, `NewReleasesHost`, `AccountPage`)
- **AND** `AppModule` carries `@ComponentScan("io.github.alexmaryin.followmymus")`

#### Scenario: DbModule declares ComponentScan for the project root package

- **WHEN** the Koin compiler plugin generates `DbModule.module` at build time
- **THEN** the generated module contains the 5 annotated repository classes (`ApiArtistsRepository`, `ApiReleasesRepository`, `ApiMediaRepository`, `ApiSyncRepository`, `RoomRepository`)
- **AND** `DbModule` carries `@ComponentScan("io.github.alexmaryin.followmymus")`

#### Scenario: A new annotated class is discovered without a module edit

- **WHEN** a new class `class MyService` annotated with `@Single` is added under `io.github.alexmaryin.followmymus.*`
- **THEN** `module.verify()` recognizes `MyService` as a registered `single` definition
- **AND** neither `AppModule` nor `DbModule` was edited to add a provider function

#### Scenario: Koin graph is identical to the pre-refactor state

- **WHEN** `module.verify()` is called on the assembled Koin graph at app startup
- **THEN** every `get<T>()` / `inject<T>()` call site in the codebase resolves a definition of the same scope (`single` or `factory`) and bound interface as before the refactor
- **AND** no runtime difference is observable compared to the pre-refactor provider-function pattern
