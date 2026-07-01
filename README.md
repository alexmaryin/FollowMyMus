<div align="center">
  <img src="composeApp/src/commonMain/composeResources/drawable/icon.png" alt="FollowMyMus Logo" width="128" height="128"/>
  <h1>FollowMyMus</h1>
  <p>
    <strong>A cross-platform music artist tracker and discovery app</strong>
    <br/>
    Search, follow, and discover new releases from your favorite music artists.
  </p>
  <p>
    <a href="#features">Features</a> •
    <a href="#screenshots">Screenshots</a> •
    <a href="#tech-stack">Tech Stack</a> •
    <a href="#getting-started">Getting Started</a> •
    <a href="#project-structure">Project Structure</a> •
    <a href="#architecture">Architecture</a>
  </p>
</div>

---

## Features

- **Artist Search & Discovery** — Search the [MusicBrainz](https://musicbrainz.org/) database for artists by name and description. Browse detailed profiles with biography, tags, and related links.
- **Release Browser** — Explore every release (albums, singles, EPs, compilations) for any artist, grouped by type with cover art from the [Cover Art Archive](https://coverartarchive.org/).
- **Track Listings** — Drill into any release to view media (CDs, vinyl, digital) with complete track listings in a responsive grid.
- **Follow Artists** — Save artists to your favorites list with one tap. Favorites sync across all your devices via Supabase.
- **New Releases Feed** — Automatically discover new releases from your followed artists. Swipe to mark as seen or dismissed. The app batch-syncs the latest releases using MusicBrainz date-range queries.
- **Favorites Import/Export** — Export your favorites as a portable JSON file for backup or sharing. Import favorites from another device with automatic validation and merge.
- **QR Code Session Transfer** — Generate a QR code on one device, scan it on another, and instantly transfer your Supabase auth session via Realtime.
- **Cross-Platform** — Runs on Android, iOS, and Desktop (macOS, Windows, Linux) from a single shared codebase.
- **Adaptive UI** — Single-panel layout on phones, multi-panel layout on tablets and desktop.
- **Dark & Light Theme** — Material 3 theming with system/light/dark mode selection and Android Material You dynamic colors.
- **Multi-Language** — Full English and Russian translations.

## Screenshots

<p align="center">
  <em>Screenshots coming soon</em>
</p>

---

## Tech Stack

| Category | Technology | Version |
|---|---|---|
| **Language** | [Kotlin](https://kotlinlang.org/) | 2.4.0 |
| **UI Framework** | [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) | 1.11.1 |
| **Design System** | Material 3 | 1.11.0-alpha07 |
| **Navigation** | [Decompose](https://github.com/arkivanov/Decompose) | 3.5.0 |
| **Dependency Injection** | [Koin](https://insert-koin.io/) (Annotations + KSP) | 4.2.2 |
| **HTTP Client** | [Ktor](https://ktor.io/) | 3.5.1 |
| **Backend** | [Supabase](https://supabase.com/) (Auth, Realtime, PostgREST) | 3.6.0 |
| **Local Database** | [Room](https://developer.android.com/kotlin/multiplatform/room) | 2.8.4 |
| **Paging** | AndroidX Paging | 3.5.0 |
| **Image Loading** | [Coil](https://coil-kt.github.io/coil/) | 3.5.0 |
| **QR Generation** | [QRCode-Kotlin](https://github.com/g0dkar/qrcode-kotlin) | 4.5.0 |
| **QR Scanning (Android)** | CameraX + ML Kit | 1.6.1 / 17.3.0 |
| **QR Scanning (Desktop)** | ZXing | 3.5.4 |
| **Analytics** | Kotzilla | — |
| **Testing** | kotlin.test + [MockK](https://mockk.io/) | 1.14.11 |

---

## Getting Started

### Prerequisites

- **JDK 21** (enforced via `jvmToolchain(21)`)
- Android Studio Ladybug or later (for Android builds)
- Xcode 16+ (for iOS builds)
- A [Supabase](https://supabase.com/) project (for backend features)

### Setup

1. **Clone the repository**

   ```shell
   git clone https://github.com/alexmaryin/FollowMyMus.git
   cd FollowMyMus
   ```

2. **Create `local.properties`**

   The project requires a `local.properties` file at the root with your Supabase credentials:

   ```properties
   projectId=<your-supabase-project-id>
   publishableKey=<your-supabase-publishable-key>
   secretKey=<your-supabase-secret-key>
   ```

   These values are consumed by the [BuildKonfig](https://github.com/codingfeline/konfig) plugin at compile time.

3. **Android — Debug Build**

   ```shell
   ./gradlew :androidApp:assembleDebug
   ```

4. **Desktop — Run (JVM)**

   ```shell
   ./gradlew :composeApp:run
   ```

5. **iOS — Open in Xcode**

   Open the `iosApp/` directory in Xcode, select a simulator or device, and run.

### Run Tests

```shell
# All tests (JVM)
./gradlew :composeApp:allTests

# JVM tests only
./gradlew :composeApp:jvmTest

# Clean rebuild (useful after KSP / BuildKonfig changes)
./gradlew clean :composeApp:assembleDebug
```

---

## Project Structure

```
FollowMyMus/
├── composeApp/                    # Shared Kotlin Multiplatform module
│   └── src/
│       ├── commonMain/            # Shared code for all platforms
│       │   ├── kotlin/            # Source code (UI, domain, data, DI)
│       │   └── composeResources/  # Strings, icons, images, fonts
│       ├── androidMain/           # Android-specific (CameraX, ML Kit, OkHttp)
│       ├── iosMain/               # iOS-specific (Darwin Ktor engine)
│       ├── jvmMain/               # Desktop (Swing, ZXing QR scanning)
│       ├── commonTest/            # Shared tests
│       └── jvmTest/               # JVM-specific tests
├── androidApp/                    # Thin Android application wrapper
│   └── src/main/                  # Android Manifest, resources
├── iosApp/                        # iOS Xcode project (Swift entry point)
│   └── iosApp/                    # iOSApp.swift, RootView.swift
├── gradle/
│   └── libs.versions.toml         # Version catalog (all dependencies)
├── docs/
│   ├── architecture.md            # Detailed architecture documentation
│   └── issues/                    # Known issues and breaking changes
├── supabase/                      # Supabase CLI local config
├── openspec/                      # OpenSpec spec files
└── graphify-out/                  # Knowledge graph (development)
```

### Modules

| Module | Type | Responsibility |
|---|---|---|
| `:composeApp` | Shared KMP Library | All common UI, domain logic, data layer, DI |
| `:androidApp` | Android App | Thin wrapper, Android entry point |
| `iosApp/` | Xcode Project | iOS entry point, SwiftUI shell |

---

## Architecture

FollowMyMus follows a **component-based architecture** built on Decompose for navigation and state management, with Koin for dependency injection.

### Key Patterns

- **Navigation**: Decompose `ChildStack` with a root pager (Artists, Favorites, Releases, Account tabs).
- **Component Tree**: `RootComponent` → `MainRootComponent` → tab components, each managing its own `MutableValue<State>`.
- **DI**: Koin annotations (`@Module`, `@Single`, `@Factory`) with KSP code generation.
- **Paging**: A custom domain-agnostic paging layer wraps AndroidX Paging with `HandlePagingItems` for Compose, exposing a single `PagingUiState<T>` sealed type (`Loading` / `Empty` / `Error` / `Content`).
- **Network Sync**: All-pages sync pattern for releases and media — fetches pages iteratively, persists each page to Room, handles partial sync gracefully.
- **Adaptive Layout**: Panels pattern adapts between single-panel (phone) and dual/three-panel (tablet/desktop) layouts.
- **State + Action**: UI components emit sealed `Action` classes; components reduce actions into new state.

### Room Database

- 10 entities: `Area`, `Artist`, `Tag`, `Resource`, `Release`, `Media`, `MediaItem`, `Track`, `MediaResource`, `NewRelease`
- Schema version 2 with migration support
- Database schema exported to `composeApp/schemas/`

---

## API Integration

### MusicBrainz

The app queries the [MusicBrainz API](https://musicbrainz.org/ws/2) for:

- Artist search and details (with `url-rels` and `release-groups`)
- Release groups by Lucene date-range queries (new releases feed)
- Release media and recordings with track listings

**Rate limiting**: All API calls are throttled to 1 request/second via `RateLimitedApiQueue` (MusicBrainz requirement). The app identifies itself as `FollowMyMus/1.0.0 (java.ul@gmail.com)`.

### Cover Art Archive

Album cover images are fetched from the [Cover Art Archive](https://coverartarchive.org/) (release group and release endpoints).

### Supabase Backend

- **Auth**: Username/password authentication
- **PostgREST**: Favorites CRUD with row-level security
- **Realtime**: QR-code session transfer channel

### Local Caching

All API data is cached locally in Room for offline access. Network responses are persisted with paging support so artists, releases, and media are available without connectivity.

---

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

```
FollowMyMus — Music artist tracker and discovery app
Copyright (C) 2025-2026 Alex Maryin

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

---

<div align="center">
  <sub>Built with ❤️ using Kotlin Multiplatform and Compose Multiplatform</sub>
  <br/>
  <sub>Data sourced from <a href="https://musicbrainz.org/">MusicBrainz</a> — the open music encyclopedia</sub>
</div>
