# FollowMyMus — Components Architecture & Navigation

A reference for re-orienting after a long break. Focuses on **how screens are structured, how they nest, and how the slots pattern is used to pass UI between them**. Code is referenced with `file:line` so you can jump straight to the source.

---

## 0. Big picture in 30 seconds

FollowMyMus is a Kotlin Multiplatform app. The `:composeApp` module holds **all** shared UI, domain, and data — `:androidApp` and `iosApp/` are thin entry points. The app uses three core technologies that you need to keep straight:

| Concern | Technology |
|---|---|
| Navigation | **Decompose** (Arkivanov). Components, not Compose, drive navigation. |
| DI | **Koin** with `@Module` / `@Single` / `@Factory` annotations + KSP codegen. |
| UI | **Compose Multiplatform** — but every Composable observes a Decompose component. |

Mental model: there is **one root component tree** that mirrors the visible UI. Every Composable receives a `*Component` interface as a parameter, subscribes to its `Value<State>`, and sends `*Action` values back via `component(action)`. Components hold navigation state; Composables are stateless renderers.

---

## 1. Application bootstrap

`composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/core/FollowMyMusApp.kt:7` is the `@KoinApplication` entry point. Koin's codegen produces a generated `FollowMyMusApp_koin.kt` that initialises the DI graph (`AppModule` + `DbModule` + platform `appModule`). Platform entry points (Android `Application`, iOS `App.swift`, Desktop `main()`) call this generated bootstrap, then hand off to `RootContent` with a `MainRootComponent`.

---

## 2. The root navigation stack

### 2.1 Interface & state

- `rootNavigation/RootComponent.kt:9` — `interface RootComponent`:
  - `childStack: Value<ChildStack<*, Child>>` — the visible screen
  - `state: Value<RootState>` — app-wide state (theme, language, dynamic mode)
  - `operator fun invoke(action: RootAction)` — action entry point
  - `sealed class Child` — `Splash`, `LoginChild`, `SignUpChild`, `MainScreenPager`
- `rootNavigation/RootState.kt:6` — `RootState(isDark, languageTag, dynamicMode)` (`@Serializable`)
- `rootNavigation/RootAction.kt:3` — `sealed class RootAction` for theme / language / dynamic mode changes
- `rootNavigation/Config.kt:6` — `@Serializable sealed class Config` — serialised config for stack children: `Splash`, `Login(qrCode)`, `SignUp`, `MainScreen(nickname)`

### 2.2 Implementation

`rootNavigation/MainRootComponent.kt:22` is the only implementation:

- Holds a `StackNavigation<Config>` and a `childStack(...)` block that maps each `Config` to a `Child` (Decompose's "factory" lambda).
- For `Config.Splash` it **observes `SessionManager.sessionStatus()`** (`MainRootComponent.kt:75`):
  - `Authenticated` → `replaceAll(Config.MainScreen(nickname = ...))`
  - `NotAuthenticated` / `RefreshFailure` → `replaceAll(Config.Login())`
- `RootAction`s mutate the root state (`isDark`, `languageTag`, `dynamicMode`) which is then read by `PreferencesHandler` and `FollowMyMusTheme`.

### 2.3 The Composable that renders the stack

`rootNavigation/ui/RootContent.kt:22` is the single root `@Composable`:

```
RootContent(component)
 └─ PreferencesHandler(state, component::invoke)        // persists theme/lang/dynamic
 └─ FollowMyMusTheme(darkTheme, androidDynamicMode)      // Material 3 theme
     └─ AddOnlyDesktopLanguageKey(state.languageTag) {  // recompose key on desktop only
           Children(
             stack = component.childStack,
             animation = stackAnimation(slide() + fade())
           ) { when (child.instance) {
               Child.Splash             -> SplashScreen()
               Child.LoginChild         -> LoginScreen(child.component)
               Child.SignUpChild        -> SignUpScreen(child.component)
               Child.MainScreenPager    -> MainScreen(child.component)
           } }
         }
       }
```

`PreferencesHandler` (`rootNavigation/ui/PreferencesHandler.kt:1`) listens to `RootState` changes and forwards them through a `Preferences` data store (so user choices survive process death).

`AddOnlyDesktopLanguageKey` is an inline helper that wraps content in `key(languageTag) { ... }` on desktop only — desktop needs an explicit recomposition trigger when the system language changes, mobile does not.

---

## 3. The "splash" pattern

`Config.Splash` is rendered by `screens/splash/SplashScreen.kt:23` — a tiny self-contained Composable that does **no** work. It exists only because `MainRootComponent` mounts the splash **before** subscribing to `SessionManager.sessionStatus()`. The first emission navigates away, and `replaceAll` discards the splash from the back stack.

---

## 4. Login & SignUp screens

These are **stack children of the root**, not part of the main pager. Both share the same shape:

### Login

- Component interface: `screens/login/domain/LoginComponent.kt:6` — `state: Value<LoginState>`, `events: Flow<LoginEvent>`, `invoke(LoginAction)`
- Composable: `screens/login/ui/LoginScreen.kt:27`
  - Owns its own `Scaffold` + `SnackbarHost`
  - **Layout branching by device**: `currentWindowAdaptiveInfo().windowSizeClass` → `DeviceConfiguration` → one of `LoginPortrait`, `LoginPhoneLandscape`, `LoginLandscape`
  - All three variants live in the same `screens/login/ui/` package
  - Subscribes to `component.events` via `ObserveEvents` and shows a snackbar on `LoginEvent.ShowError`
  - Forwards everything to `component::invoke` (with a side-effect wrapper that hides the keyboard and commits autofill before the `OnLogin` action reaches the component)
- Config carries an optional `qrCode: String?` (used for deep-link login): `Config.Login(qrCode = null)`

### SignUp

Mirror of Login. `screens/signUp/ui/SignUpScreen.kt:21` has the same `Scaffold` + `SnackbarHost` + portrait/landscape/desktop branching. `Config.SignUp` is a `data object` (no parameters).

Navigation between Login ↔ SignUp is done in the root stack by passing lambdas to the components (`MainRootComponent.kt:46` for `onSignUpClick`, `:54` for `onLoginClick`).

---

## 5. The MainScreen — pager with slots

After successful auth, the root pushes `Config.MainScreen(nickname)` and the user enters the "real" app.

### 5.1 The pager component

- Interface: `screens/mainScreen/domain/mainScreenPager/PagerComponent.kt:8`
  - `pages: Value<ChildPages<*, Page>>` — Decompose pager
  - `state: Value<MainScreenState>`
  - `invoke(MainScreenAction)`
- Implementation: `screens/mainScreen/domain/mainScreenPager/MainPagerComponent.kt:25`
  - `@Factory(binds = [PagerComponent::class])` — Koin instantiates it with `(componentContext, nickname)`
  - `PagesNavigation<PagerConfig>()` + `childPages(...)` — the four pages are `PagerConfig.Artists`, `Releases`, `Favorites`, `Account` (see `PagerConfig.kt:21`)
  - Initial state: `selectedIndex = MainPages.FAVORITES.index`
  - The factory maps each `PagerConfig` to a `Page` instance:
    - `Artists`   → `get<ArtistsHostComponent>(context)` (Koin DI)
    - `Favorites` → `get<FavoritesHostComponent>(context, state.value.nickname)`
    - `Account`   → `get<AccountHostComponent>(context, state.value.nickname)`
    - `Releases`  → `get<NewReleasesHostComponent>(context)` (Koin DI)
  - `MainScreenAction.SelectPage(index)` updates `state.activePageIndex` and calls `navigation.select(index)` — both stay in sync

### 5.2 The Pager Composable

`screens/mainScreen/ui/MainScreen.kt:26` — note the nesting:

```
MainScreen(component: PagerComponent)
 └─ Scaffold(
      topBar      = TopAppBar( leadingIcon, titleContent, actions )   ← from current page's slots
      bottomBar   = BottomNavBar(selected, onPageClick)
      snackbarHost = SnackbarHost(snackBarHostState)
      fab         = currentPage.scaffoldSlots.fabContent()
    ) { padding ->
        ChildPages(pages, scrollAnimation) { index, page ->
            when (index) {
                ARTISTS   -> ArtistsPageHostUi(page as ArtistsHostComponent)
                FAVORITES -> FavoritesPageHostUi(page as FavoritesHostComponent)
                RELEASES  -> NewReleasesPageHostUi(page as NewReleasesHostComponent)
                ACCOUNT   -> AccountPageUi(page as AccountHostComponent)
            }
        }
    }
```

Two important things:

1. `LaunchedEffect(currentPage?.key)` (`MainScreen.kt:34`) collects `currentPage.scaffoldSlots.snackbarMessages` into the shared `SnackbarHostState`. **Snackbars bubble up from the active page's slots.**
2. The `topBar` slots (`leadingIcon`, `titleContent`, `trailingIcon`) and `fabContent` come from `currentPage.scaffoldSlots`. This is the heart of the slots pattern — see §7.

`BottomNavBar` (`ui/BottomNavBar.kt:10`) is a pure Composable — it iterates `MainPages.entries` and renders four `NavigationBarItem`s with active/inactive icon resources. Tapping dispatches `MainScreenAction.SelectPage(index)`.

---

## 6. The four pages and their hosts

There are **three different host shapes** under the pager:

| Page | Host shape | Reason |
|---|---|---|
| Artists | Adaptive **panels** (main + details + extra) | Drill-down: list → releases → media |
| Favorites | Adaptive **panels** (main + details + extra) | Same drill-down, from a different starting list |
| Account | Nested **stack** (Account ⇄ About ⇄ Privacy) | Sub-screens with their own back stack |
| Releases | Adaptive **panels** (main + details) | Panel host: list → media details |

The `Page` interface (`screens/mainScreen/domain/mainScreenPager/Page.kt:7`) is the contract every host satisfies:

```kotlin
interface Page {
    val scaffoldSlots: ScaffoldSlots
    val events: Channel<SnackbarMsg>
    val key: String
}
```

Every host must:
- Expose its own `ScaffoldSlots` (so the outer Scaffold can show its title, leading icon, trailing icons, FAB)
- Expose an `events: Channel<SnackbarMsg>` (consumed by the outer Scaffold's `LaunchedEffect`)
- Have a unique `key` (for `ChildPages` and `LaunchedEffect`)

---

## 7. The slots pattern in detail

The slots pattern is what lets a single `Scaffold` (the pager's) host the title / leading icon / trailing icons / FAB of the currently visible page, without each page having to own its own Scaffold.

### 7.1 The contract

`screens/mainScreen/domain/ScaffoldSlots.kt:12`:

```kotlin
interface ScaffoldSlots {
    val titleContent:  @Composable () -> Unit
    val leadingIcon:   @Composable () -> Unit
    val trailingIcon:  @Composable RowScope.() -> Unit   // <-- RowScope, not bare
    val fabContent:    @Composable () -> Unit
    val snackbarMessages: Flow<SnackbarMsg>
}

data class SnackbarMsg(val key: String, val message: String)

object DefaultScaffoldSlots : ScaffoldSlots { /* empty stubs, app-name title */ }
```

`DefaultScaffoldSlots` is a no-op implementation that delegates to `DefaultScaffoldSlots` via `ScaffoldSlots by DefaultScaffoldSlots` — most slots only override the ones they care about.

`trailingIcon` carries a `RowScope` receiver because the trailing actions live inside the `TopAppBar.actions` slot, which provides a `RowScope`.

### 7.2 How a host exposes its slots

Each host (Artists, Favorites, Account, plus the inner panels) has a `*HostSlots` class next to its `*Host.kt`. Examples:

- `pages/artists/ui/ArtistsHostSlots.kt:20` — overrides `snackbarMessages` (relays the page's own events), `leadingIcon` (Back icon when `state.backVisible`), `titleContent` (**delegates to whichever sub-panel is on top**), `trailingIcon` (similarly). It is constructed in `ArtistsHost.kt:33` with `override val scaffoldSlots = ArtistsHostSlots(this)`.
- `pages/favorites/ui/FavoriteHostSlots.kt:25` — same shape. `trailingIcon` is the sync `Avatar` (re-uses `nicknameAvatar/Avatar`).
- `pages/sharedPanels/ui/releasesPanel/ReleasesPanelSlots.kt:13` — overrides `titleContent` to a `ReleasesListTitle` (sorting + refresh). Constructed in `ReleasesList.kt:41`.
- `pages/sharedPanels/ui/mediaPanel/MediaPanelSlots.kt:15` — overrides only `titleContent` (release name). Constructed in `MediaDetails.kt:30`.
- `pages/artists/ui/artistsPanel/ArtistsPanelSlots.kt:14` — overrides `titleContent` (search bar) and `trailingIcon` (favorite icon for the open artist). Constructed in `ArtistsList.kt:32`.
- `pages/favorites/ui/favoritesPanel/FavoritesPanelSlots.kt:11` — overrides `titleContent` (sorting + refresh of the *open* artist). Note the `onRefreshReleases` parameter — it forwards to the host action.
- `pages/account/ui/AccountPageSlots.kt:10` — overrides only `leadingIcon` (Back icon). Constructed in `AccountPage.kt:49`.
- `pages/newReleases/ui/NewReleasesHostSlots.kt:16` — same delegation pattern as Artists/Favorites but only two panels. Constructed in `NewReleasesHost.kt:33`.
- `pages/newReleases/ui/NewReleasesPanelSlots.kt:11` — overrides `titleContent` with release page title + "Last synced" subtitle. Constructed in `NewReleasesList.kt:34`.

**Rule of thumb**: every component that implements `Page` constructs its `*HostSlots` and exposes it as `scaffoldSlots`. The pager's outer Scaffold picks the active page's slots and renders them. This is how the TopAppBar changes from "search bar" on Artists to "sorting" on Favorites to "release name" on Media details, with no per-screen Scaffold.

### 7.3 Slot delegation chains (the tricky part)

The outer Scaffold sees only one level of slots. But the title you see on screen can come from a sub-panel three levels deep, because the **host** slots delegate to the **current child** slots.

In Artists (`ArtistsHostSlots.kt:36`):

```kotlin
titleContent = @Composable {
    val panelsState by component.panels.subscribeAsState()
    val releasesPanel = panelsState.details?.instance
    val mediaPanel = panelsState.extra?.instance
    val singleMode = panelsState.mode == ChildPanelsMode.SINGLE
    val title = when {
        mediaPanel != null -> mediaPanel.scaffoldSlots.titleContent      // top
        singleMode && releasesPanel != null -> releasesPanel.scaffoldSlots.titleContent
        else -> panelsState.main.instance.scaffoldSlots.titleContent     // bottom
    }
    title()
}
```

The same pattern is used for `trailingIcon`. **Favorites host uses the same pattern in `FavoriteHostSlots.kt:42`**.

`ChildPanelsMode` is set from the window size class — see `pages/artists/ui/ArtistsPageHostUi.kt:55` (a `DisposableEffect` that dispatches `SetMode(mode)` on every window-size change). This is how the adaptive layout works.

---

## 8. Artists host — the panel host in detail

This is the canonical "panels" pattern. Favorites is structurally identical.

### 8.1 Component hierarchy

```
ArtistsHostComponent (Page)                                  ← pages/artists/domain/panelsNavigation/ArtistsHostComponent.kt:15
 └─ Panels<Unit, ReleasesConfig, MediaDetailsConfig>         ← 3 slots: main / details / extra
     ├─ main (always Unit) -> ArtistsList                   ← artistsListPanel/ArtistsList.kt:24
     │   └─ Pager<Artist> + search bar + favorite icon
     ├─ details (optional) -> ReleasesList                  ← sharedPanels/domain/releasesPanel/ReleasesList.kt:23
     │   └─ Pager<GroupedItem<Release, ...>> + cover viewer
     └─ extra (optional)   -> MediaDetails                  ← sharedPanels/domain/mediaDetailsPanel/MediaDetails.kt:21
         └─ Pager<Media> + staggered grid
```

### 8.2 The host

`pages/artists/domain/pageHost/ArtistsHost.kt:25` (`@Factory`):

- `PanelsNavigation<Unit, ReleasesConfig, MediaDetailsConfig>()` — the navigation handle
- `childPanels(source, serializers, initialPanels = { Panels(main = Unit) }, ...)` with three factories:
  - `mainFactory`     → `ArtistsList(get(), context, ::invoke)`
  - `detailsFactory`  → private `getReleasesList(config, context)` — uses the `artistId` and `artistName` from the config
  - `extraFactory`    → private `getMediaDetails(config, context)` — uses the `releaseId` and `releaseName` from the config
- `onStateChanged` callback updates local state: `artistIdSelected`, `releaseIdSelected`, `backVisible` (back is visible iff there is an extra panel, or a details panel in `SINGLE` mode)
- `invoke(ArtistsHostAction)` dispatches panel navigation:
  - `ShowReleases(artistId, artistName)` → `state.copy(details = ReleasesConfig(...))`
  - `ShowMediaDetails(releaseId, releaseName)` → `state.copy(extra = MediaDetailsConfig(...))`
  - `CloseReleases` / `CloseMediaDetails` → `dismissDetails` / `dismissExtra`
  - `SetMode(mode)` → updates `state.copy(mode = mode)`
  - `OnBack` → custom `onBack()` that deselects the active release/artist and then `navigation.pop()`

### 8.3 Configuration types

`pages/artists/domain/panelsNavigation/ArtistsPanelConfig.kt:7`:
- `data object ListConfig` (used as `Unit` in Artists) — note: Artists uses raw `Unit` for its main panel, but Favorites uses a `ListConfig` data object
- `data class ReleasesConfig(artistId, artistName)`
- `data class MediaDetailsConfig(releaseId, releaseName)`
- `SERIALIZERS: Triple(Unit.serializer(), ReleasesConfig.serializer(), MediaDetailsConfig.serializer())` — passed to `childPanels` for state restoration

### 8.4 UI

`pages/artists/ui/ArtistsPageHostUi.kt:31` is a Composable that does **only one thing**: renders the adaptive `ChildPanels` from Decompose. It also reads the window size class and dispatches `SetMode` (SINGLE on phones, DUAL on expanded width).

`ChildPanels` is called with:
- `mainChild`     → `ArtistsPanelUi(it.instance)`
- `detailsChild`  → `ReleasesPanelUi(it.instance)`
- `extraChild`    → `MediaPanelUi(it.instance)`
- `layout = HorizontalChildPanelsLayout(dualWeights = 0.4f to 0.6f)` (main gets 40% of width in DUAL)
- `secondPanelPlaceholder = { ReleasesPlaceholder() }` — shown when the user hasn't opened releases yet
- `animators` and `predictiveBackParams` — predictive back gesture support

### 8.5 Inner panels

`ArtistsPanelUi` (`ui/artistsPanel/ArtistsPanelUi.kt:44`):
- Renders a `LazyColumn` of `ArtistListItem`s via `HandlePagingItems`
- Has an FAB that scrolls to top when scrolled down
- Manages its own "scroll up on search completed" effect by collecting `component.listEvents` (a `SharedFlow<ArtistsListEvent>`)
- Empty / loading / error states are routed through the `HandlePagingItems` DSL

`ReleasesPanelUi` (`ui/releasesPanel/ReleasesPanelUI.kt:23`):
- Reads `component.resources` (a `Map<String, List<Resource>>` of artist images) and `component.releases` (a `PagingData<GroupedItem<Release, ...>>`)
- Renders a `PullToRefreshMobile` (only on mobile)
- Switches between `ReleasesPanelTall` (portrait) and `ReleasesPanelLandscape` (landscape / wide) based on `DeviceConfiguration`
- The full-screen cover viewer (when `state.openedCover` is set) is rendered as a sibling to `HandlePagingItems`

`MediaPanelUi` (`ui/mediaPanel/MediaPanelUi.kt:38`):
- Renders a `LazyVerticalStaggeredGrid` of cover thumbnails
- Tapping a thumbnail opens a `Dialog` with the track listing
- Hosted in the `extra` panel — its slots title (`MediaPanelSlots.kt:21`) shows the release name; the host's `titleContent` slots delegate to it when present

### 8.6 Per-list components

Each list component (`ArtistsList`, `ReleasesList`, `MediaDetails`, `FavoritesList`, `NewReleasesList`) follows the same shape:

```
class XxxList(...) : Page, ComponentContext by context {
    override val key = "XxxList"
    override val events = Channel<SnackbarMsg>()
    override val scaffoldSlots = XxxPanelSlots(this)

    private val _state by saveableMutableValue(XxxListState.serializer(), init = ::XxxListState)
    val state: Value<XxxListState> = _state

    // paged flow exposed to the UI
    val items: Flow<PagingData<...>> = repository.getXxx(...).map { ... }

    operator fun invoke(action: XxxListAction) { ... }  // state mutations + hostAction callbacks

    init {
        lifecycle.doOnStart {
            // one-shot API sync if local cache is empty
            // observe repository.workState → emit SnackbarMsg on PARTIAL_SYNC
            // observe repository.errors → emit SnackbarMsg on errors
        }
    }
}
```

Each has its own `*Action` (sealed class/interface) and `*State` (`@Serializable data class`) in the same `domain/` package. Saveable state uses `saveableMutableValue(serializer, init)` from `core/data/`.

---

## 9. Favorites host — same shape, different start

`pages/favorites/domain/panelsNavigation/FavoritesHostComponent.kt:15` mirrors the Artists host interface but with `FavoritesList` as the main panel. Notable differences:

- The host **also subscribes to the global sync** (`SyncRepository`) — see `pages/favorites/domain/pageHost/FavoritesHost.kt:93` and the `avatar.isSyncing` flag. Tapping the avatar in the trailing slot dispatches `FavoritesHostAction.SyncRequested`.
- It collects snackbars from **all three** of its panels and re-emits them on its own `events` channel (`FavoritesHost.kt:80`): the outer Scaffold then displays them.
- Main panel factory uses `FavoritesPanelConfig.ListConfig` (a data object) instead of `Unit` — config-driven rather than value-driven.
- `FavoriteHostSlots.kt:50` puts the `Avatar` (sync button) in the trailing slot, not in the leading slot.
- `FavoritesList` (`favoritesPanel/FavoritesList.kt:26`) groups the paged items by `SortArtists` and shows a sticky group header; a `ConfirmationDialog` is rendered when removing a favorite.

---

## 10. Account — nested stack

The Account page does **not** use panels. It uses a small **inner stack** for "Preferences ⇄ About ⇄ Privacy Policy".

- Component: `pages/account/domain/nestedNavigation/AccountHostComponent.kt:8` extends `Page` and exposes:
  - `childStack: Value<ChildStack<*, Child>>` where `Child` is `Account(nickname) | PrivacyPolicy | About`
  - `state: Value<AccountPageState>`
- Implementation: `pages/account/domain/AccountPage.kt:37` (`@Factory`)
  - `StackNavigation<AccountPageConfig>()` + `childStack(...)` with three configs from `AccountPageConfig.kt:6`
  - `invoke(AccountAction)` handles: `ShowAbout`, `ShowPrivacyPolicy`, `Logout` (calls `repository.clearLocalData()` + `sessionManager.signOut()`), `ToggleQrView` (starts/closes a Supabase Realtime channel for QR session transfer), `LanguageClick` / `ThemeClick` / `DynamicClick` (toggle modals), `LanguageChange` / `ThemeChange` / `DynamicChange` (write through to preferences), `DownloadQR` (saves the QR image via `FileHandler`), `OnBack`
  - The QR transfer (`AccountPage.kt:121`) creates a UUID, opens a Supabase Realtime channel, and uses `startTransferSession(sessionManager)` to deliver a fresh session
- UI: `pages/account/ui/AccountPageUi.kt:15` — `Children(stack, animation) { when (it.instance) { About | Account | PrivacyPolicy } }`
  - `AboutUi` and `PrivacyPolicyUi` (`parts/AboutUi.kt:16`, `PrivacyPolicyUi.kt:16`) are tiny screens that render `parseSimpleMarkdown(stringResource(...))` in a vertical scroll
  - `PreferencesUi` (`parts/PreferencesUi.kt:37`) is the **busiest** screen in the app:
    - Owns three modal sheets: `ThemeModalUi`, `LanguageModalUi`, `DynamicModalUi` (gated by `state.isXxxModalOpened`)
    - Owns the logout `ConfirmationDialog` and `LogoutShimmer` overlay (shown while `state.sessionLogout`)
    - Renders `AccountCaption` + `UserListItem` (nickname + QR toggle + logout)
    - The QR is rendered by `components/QRGeneratedBlock.kt:32` with a 60-second expiry animation
    - Two `PreferencesGroup`s: "App" (language, theme, dynamic) and "About" (privacy, about, version)
    - iOS-specific path: language preference uses `changeLanguage(null)` (iOS only) instead of opening a modal
  - Slots: only `leadingIcon` is overridden (`AccountPageSlots.kt:10`) — shows `BackIcon` when `state.backVisible`

---

## 11. The Releases page — panel host (main + details)

`pages/newReleases/domain/panelsNavigation/NewReleasesHostComponent.kt:15` is the `Page` interface for the Releases tab. It follows the same panels pattern as Artists/Favorites but with only two slots: **main** (the grouped release list) and **details** (media details).

### Component hierarchy

```
NewReleasesHostComponent (Page)                                ← pages/newReleases/domain/panelsNavigation/NewReleasesHostComponent.kt:15
 └─ Panels<Unit, MediaDetailsConfig, Unit>                     ← 2 active slots: main / details (extra is Unit)
     ├─ main -> NewReleasesList                                ← domain/list/NewReleasesList.kt:26
     │   └─ Pager<GroupedItem<Release, ...>> + swipeable cards
     └─ details (optional) -> MediaDetails                     ← sharedPanels/domain/mediaDetailsPanel/MediaDetails.kt:21
         └─ Pager<Media> + staggered grid
```

### Key differences from Artists/Favorites

- **Two panels, not three**: only main (list) and details (media). No extra panel.
- **Swipeable items**: each release row uses `SwipeToDismissBox` to mark releases as `DISMISSED` (one-way state: `UNSEEN → SEEN → DISMISSED`).
- **Auto-sync on first open**: `NewReleasesList.init` triggers `syncNewReleases()` which batches all favorite artist IDs (batch of 50), builds a Lucene `firstreleasedate:[floor TO today]` query, and paginates through MusicBrainz. The floor advances each sync so only new releases are fetched.
- **Grouped by artist name**: the paged stream uses `groupedBy { it.artistName }` so the LazyColumn shows artist-name headers above each group.
- **No trailing icon**: the host slots only override `titleContent` (delegating to the active panel) and `leadingIcon` (Back when details is open). The list panel slots show a "Last synced" subtitle.
- **WorkState and errors**: collected from the repository, surfaced as snackbars in the host's `events` channel.

---

## 12. Shared UI helpers (`core/ui/`)

These show up in many screens. Skim these once and you can read the rest.

- `HandlePagingItems<T>` (`pagingHandler.kt:101`) — DSL wrapper for `LazyPagingItems<T>`. Computes a single `PagingUiState<T>` (`Loading` / `Empty` / `Error` / `Content`) per recomposition and dispatches to a mutually-exclusive branch. Provides:
  - `OnLoading { }`, `OnEmpty { }`, `OnError { PagingError -> }`, `OnContent { LazyPagingItems<T> -> }`
  - `LazyListScope.onPagingItems(key, contentType) { T -> }` — DSL inside `OnContent`
  - `LazyListScope.onAppendLoading { }`, `onAppendError { (PagingError, retry) -> }`, `onLastItem { }`
  - Optional `errorMapper` and `onErrorAction` (for "refresh failed on populated list" snackbars)
- `ObserveEvents(flow, onEvent)` (`ObserveEvents.kt:13`) — `LaunchedEffect` + `repeatOnLifecycle(STARTED)` that collects a flow on the main dispatcher. Used to subscribe to a component's `events` from a Composable.
- `PullToRefreshMobile` (`PullToRefreshMobile.kt:1`) — platform-aware pull-to-refresh (different impls per source set).
- `VinylLoadingIndicator` (`VinylLoadingIndicator.kt:1`) — themed loading spinner.
- `DeviceConfiguration` (`DeviceConfiguration.kt:1`) — `MOBILE_PORTRAIT | MOBILE_LANDSCAPE | TABLET_PORTRAIT | TABLET_LANDSCAPE | DESKTOP`. Derived from `currentWindowAdaptiveInfo().windowSizeClass`.
- `parseSimpleMarkdown(text)` (`markdownParser.kt:1`) — small markdown subset for About / Privacy screens.
- `isAndroid()` / `isIOS()` / `isDesktop()` — platform predicates in `Platform.kt`.
- `modifiers/`, `theme/` — Material 3 theme and small modifier extensions.

`commonUi/` (`screens/commonUi/`) holds truly cross-screen atoms:
- `BackIcon` (used in `*HostSlots.leadingIcon` overrides)
- `BaseTextField`, `BaseSecureField` (text inputs with custom theming)
- `ConfirmationDialog` (used in Favorites removal + Account logout)
- `EmptyListPlaceholder`, `ErrorPlaceholder` (used inside `HandlePagingItems.OnEmpty` / `.OnError`)
- `SoftCornerBlock` (visual container used in `PreferencesUi`)
- `TextWithLink` (linkified text)
- `LogoAnimation` (animated logo)

---

## 13. The state + action convention

Almost every component uses the same triplet of files in its `domain/` package:

| File | Purpose |
|---|---|
| `XxxComponent.kt` | The interface; exposes `state: Value<XxxState>`, action entry point, possibly `events: Flow<XxxEvent>` |
| `XxxState.kt` | `@Serializable data class XxxState(...)` — all UI state goes here, marked saveable |
| `XxxAction.kt` | `sealed interface XxxAction` — every user intent is a case of this |
| (sometimes) `XxxEvent.kt` | `sealed interface XxxEvent` — one-off side effects (snackbars, navigation) |

The Composable is **stateless**: it `subscribeAsState()`s the `Value<XxxState>`, renders, and sends `*Action` values back via `component::invoke` or a hoisted `onAction: (X) -> Unit` lambda.

---

## 14. Koin DI quick reference

- `@KoinApplication` on `FollowMyMusApp` (`core/FollowMyMusApp.kt:7`) declares the global modules.
- `@Module` declares DI modules (`AppModule`, `DbModule`).
- `@Single` → application-scoped singleton (e.g. `SupabaseClient`, repositories)
- `@Factory` → new instance per resolve (e.g. `MainPagerComponent`, `ArtistsHost`, `FavoritesHost`, `NewReleasesHost`, `AccountPage`)
- `KoinComponent` + `inject<T>()` or `get<T> { parametersOf(...) }` to resolve in the constructor / factory function
- `parametersOf(...)` is how Decompose passes `ComponentContext` (and any other params) into a `KoinComponent`-aware factory

`MainRootComponent.kt:63,68,73` shows the typical pattern: a private function that wraps `get<XxxComponent> { parametersOf(...) }`, called from the `childStack` factory block.

---

## 15. File map (where to look first)

```
composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/
├── rootNavigation/                 ← ROOT STACK
│   ├── RootComponent.kt            ← interface
│   ├── MainRootComponent.kt        ← implementation (also observes SessionManager)
│   ├── RootState.kt / RootAction.kt
│   ├── Config.kt                   ← serialised stack configs
│   └── ui/
│       ├── RootContent.kt          ← the one root Composable
│       └── PreferencesHandler.kt
│
├── screens/
│   ├── splash/SplashScreen.kt      ← pure visual; not a component
│   ├── login/
│   │   ├── domain/LoginComponent.kt
│   │   └── ui/LoginScreen.kt + LoginPortrait.kt + LoginPhoneLandscape.kt + LoginLandscape.kt
│   ├── signUp/                     ← mirror of login
│   └── mainScreen/
│       ├── domain/
│       │   ├── MainScreenState.kt / MainScreenAction.kt / ScaffoldSlots.kt
│       │   └── mainScreenPager/
│       │       ├── PagerComponent.kt / MainPagerComponent.kt
│       │       ├── PagerConfig.kt / Page.kt / MainPages enum
│       ├── ui/
│       │   ├── MainScreen.kt       ← outer Scaffold + ChildPages
│       │   └── BottomNavBar.kt
│       └── pages/
│           ├── artists/            ← PANEL HOST
│           │   ├── domain/
│           │   │   ├── pageHost/ArtistsHost.kt (impl) + Action + State
│           │   │   ├── artistsListPanel/ArtistsList.kt + Action + State + Event
│           │   │   ├── panelsNavigation/ArtistsHostComponent.kt + ArtistsPanelConfig.kt
│           │   │   └── models/
│           │   └── ui/
│           │       ├── ArtistsPageHostUi.kt        ← ChildPanels
│           │       ├── ArtistsHostSlots.kt         ← outer slots
│           │       └── artistsPanel/
│           │           ├── ArtistsPanelUi.kt
│           │           ├── ArtistsPanelSlots.kt     ← inner slots
│           │           └── components/...
│           ├── favorites/          ← PANEL HOST (same shape as artists, starts with sync)
│           ├── newReleases/        ← PANEL HOST (main + details, swipeable grouped list)
│           │   ├── domain/
│           │   │   ├── pageHost/NewReleasesHost.kt (impl) + Action + State
│           │   │   ├── list/NewReleasesList.kt + Action + State
│           │   │   ├── panelsNavigation/NewReleasesHostComponent.kt + NewReleasesPanelConfig.kt
│           │   └── ui/
│           │       ├── NewReleasesPageHostUi.kt         ← ChildPanels
│           │       ├── NewReleasesHostSlots.kt          ← outer slots
│           │       ├── NewReleasesPanelSlots.kt         ← inner slots
│           │       └── list/NewReleasesList.kt          ← grouped paged + swipeable cards
│           ├── account/            ← NESTED STACK
│           │   ├── domain/AccountPage.kt + AccountPageState.kt
│           │   ├── domain/nestedNavigation/AccountHostComponent.kt + AccountAction.kt + AccountPageConfig.kt
│           │   └── ui/AccountPageUi.kt + AccountPageSlots.kt
│           │       └── parts/PreferencesUi.kt + AboutUi.kt + PrivacyPolicyUi.kt + 4 modal sheets + LogoutShimmer
│           │       └── components/...           ← AccountCaption, UserListItem, QRGeneratedBlock, ...
│           └── sharedPanels/       ← USED BY Artists AND Favorites
│               ├── domain/releasesPanel/ReleasesList.kt + Action + State
│               ├── domain/mediaDetailsPanel/MediaDetails.kt + Action + State
│               └── ui/releasesPanel/ReleasesPanelUI.kt + Tall + Landscape + Slots
│               └── ui/mediaPanel/MediaPanelUi.kt + MediaPanelSlots.kt
│
├── core/                           ← shared, app-wide
│   ├── FollowMyMusApp.kt           ← @KoinApplication
│   ├── di/                         ← AppModule, DbModule
│   ├── data/                       ← saveableMutableValue, asFlow
│   ├── paging/                     ← PagingDefaults, PagingError, PagingDataExt, PagingDataGrouping
│   ├── system/                     ← platform file pickers, etc.
│   ├── ui/                         ← HandlePagingItems, ObserveEvents, PullToRefresh, ...
│   └── changeLanguage.kt / resultApi.kt
│
├── musicBrainz/                    ← API + repository layer (not navigation)
├── supabase/                       ← auth + realtime + postgrest wiring
├── sessionManager/                 ← Supabase session + QR deep-link
└── preferences/                    ← DataStore-backed user preferences
```

---

## 16. Mental checklist when adding a new screen

1. **Where does it live?**
   - One-off (auth, splash) → add to `rootNavigation/Config.kt` + a `Child` case in `RootComponent.kt` + a screen in `screens/<name>/`.
   - Tab in the main app → add a `PagerConfig` case + a `MainPages` entry + a host component.
   - Sub-screen of an existing host → add a `Child` to that host's `ChildStack` (stack) or `*PanelConfig` (panels).

2. **Interface first.** Create `XxxComponent.kt` extending `Page` if it's a pager page, or just declaring `state + invoke(action)`. Add `@Factory(binds = [XxxComponent::class])` on the implementation.

3. **State + Action + Event.** Three files, three sealed types, all `@Serializable` if they need to be saved.

4. **UI**: build the `XxxScreen.kt` Composable that:
   - `subscribeAsState()`s `component.state`
   - Calls `HandlePagingItems` if paginated
   - Calls `ObserveEvents(component.events)` if there are one-shot events
   - Forwards everything via `component::invoke` or an `onAction: (XxxAction) -> Unit` lambda

5. **Slots**: implement `XxxHostSlots` (or `XxxPanelSlots` for inner panels) as `: ScaffoldSlots by DefaultScaffoldSlots` — only override what the screen contributes. Construct it in the component impl: `override val scaffoldSlots = XxxHostSlots(this)`.

6. **If it's a panel host** (panels: main + details +/- extra): use `childPanels(source, serializers, initialPanels, mainFactory, detailsFactory, extraFactory)` with `*PanelConfig` triple. Set `extraFactory` to `null` for 2-panel hosts like NewReleases. Add a `SetMode` action and a `DisposableEffect(windowSize)` to drive `ChildPanelsMode`.

7. **If it has a nested stack**: use `childStack(source, serializer, initialConfiguration, factory)` with `@Serializable` configs and `bringToFront` for navigation. Example: `AccountPage`.

8. **Wire it into Koin** — `@Factory(binds = [XxxComponent::class])` + `parametersOf(componentContext, ...)` in the parent factory.

---

## 17. Quick reference: key types at a glance

| Type | Where | Role |
|---|---|---|
| `RootComponent` / `MainRootComponent` | `rootNavigation/` | Root stack (Splash/Login/SignUp/MainScreen) |
| `PagerComponent` / `MainPagerComponent` | `screens/mainScreen/domain/mainScreenPager/` | The bottom-tab pager |
| `Page` | `screens/mainScreen/domain/mainScreenPager/Page.kt` | Contract every pager child satisfies |
| `ScaffoldSlots` | `screens/mainScreen/domain/ScaffoldSlots.kt` | Contract for slots the outer Scaffold reads |
| `ArtistsHostComponent` / `FavoritesHostComponent` | `pages/{artists,favorites}/domain/panelsNavigation/` | Panel hosts (main + details + extra) |
| `NewReleasesHostComponent` | `pages/newReleases/domain/panelsNavigation/` | Panel host (main + details) |
| `AccountHostComponent` | `pages/account/domain/nestedNavigation/` | Nested stack host (Account/About/Privacy) |
| `MainPages` | `screens/mainScreen/domain/mainScreenPager/PagerConfig.kt:34` | The four tab entries (enum, with resources) |
| `PagerConfig` | same file:21 | The sealed serialised pager config |
| `*PanelConfig` | per-host | The sealed serialised panels (list/releases/media) |
| `*Action` / `*State` / `*Event` | per-component | The state-action-event triplet |
| `SnackbarMsg(key, message)` | `ScaffoldSlots.kt:20` | The one cross-cutting event type |

---

## 18. Things to remember

- **Decompose owns the back stack.** Back button handling is per-stack (`handleBackButton = true` on every `childStack` and `childPanels`).
- **`handleBackButton = true` on the pager** (`MainPagerComponent.kt:49`) — that's the phone back button on the bottom nav.
- **Predictive back gesture** is wired in `ArtistsPageHostUi.kt:46`, `FavoritesPageHostUi.kt:46`, and `NewReleasesPageHostUi.kt:46` via `PredictiveBackParams`.
- **The slots pattern is recursive.** The outer Scaffold reads `currentPage.scaffoldSlots`; the host slots delegate to whichever sub-panel is active. The leaf-level slots (`ArtistsPanelSlots`, `ReleasesPanelSlots`, `MediaPanelSlots`, `FavoritesPanelSlots`, `NewReleasesPanelSlots`) provide the actual UI.
- **Every `Page` is also a `ComponentContext`**. Use `context.coroutineScope()` for lifecycles, `lifecycle.doOnStart { ... }` for one-shot startup effects, and `saveableMutableValue(...)` for state.
- **Snackbars travel up the host chain.** Each inner panel has its own `events` channel; the host either relays them through its own `events` (Favorites) or just re-exposes them via `slots.snackbarMessages` (Artists). The outer Scaffold's `LaunchedEffect(currentPage?.key)` collects them.
- **All UI state is `@Serializable`.** Process death restoration works because every state is saved through Decompose + `saveableMutableValue`, and every navigation config is serialised.
- **DI is Koin with KSP.** Don't write `module { }` blocks by hand — use `@Module`/`@Single`/`@Factory` annotations and let the codegen wire it up. Generated code lives in `build/generated/ksp/metadata/commonMain/kotlin`.

---

This document maps the **what** of every screen and the **how** of the navigation. With this and `AGENTS.md` you should be back up to speed in an afternoon.
