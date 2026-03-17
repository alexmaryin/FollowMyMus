# FollowMyMus - Kotlin Multiplatform Project

## Project Overview

**FollowMyMus** is a cross-platform music tracking application built with **Kotlin Multiplatform** and **Compose Multiplatform**. The app targets **Android**, **iOS**, and **Desktop (JVM)** platforms, sharing UI and business logic across all targets.

### Purpose
The application integrates with the **MusicBrainz API** for music metadata and uses **Supabase** for backend services (authentication, realtime updates, and database). Users can track artists, releases, and manage favorites.

### Architecture
- **Navigation**: Decompose library for multiplatform navigation and lifecycle management
- **DI**: Koin for dependency injection with KSP code generation
- **Backend**: Supabase (Auth, Postgrest, Realtime)
- **API Client**: Ktor for HTTP requests to MusicBrainz and Supabase
- **Local Database**: Room (multiplatform) with SQLite
- **State Management**: Compose with ViewModel pattern
- **Feature Modules**: Organized by screen/feature with domain-driven design (ui/domain/data layers)

## Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin 2.3.10 |
| **Multiplatform** | Kotlin Multiplatform + Compose Multiplatform 1.11.0-alpha04 |
| **Navigation** | Decompose 3.4.0 |
| **DI** | Koin 4.1.1 + Koin Annotations 2.3.1 |
| **Backend** | Supabase 3.4.1 (Auth, Realtime, Postgrest) |
| **HTTP Client** | Ktor 3.4.1 |
| **Local DB** | Room 2.8.4 + SQLite 2.6.2 |
| **Image Loading** | Coil 3.4.0 |
| **QR Code** | ZXing (Desktop), ML Kit + CameraX (Android) |
| **Analytics** | Kotzilla SDK 2.0.9 |
| **Serialization** | Kotlinx Serialization |
| **Coroutines** | Kotlinx Coroutines 1.10.2 |

## Project Structure

```
FollowMyMus/
├── composeApp/              # Shared code module
│   ├── src/
│   │   ├── commonMain/      # Shared across all platforms
│   │   ├── androidMain/     # Android-specific implementations
│   │   ├── iosMain/         # iOS-specific implementations
│   │   ├── jvmMain/         # Desktop (JVM) implementations
│   │   └── commonTest/      # Shared tests
│   └── build.gradle.kts
├── androidApp/              # Android application entry point
│   └── build.gradle.kts
├── iosApp/                  # iOS application (Xcode project)
│   └── iosApp/
├── gradle/
│   └── libs.versions.toml   # Version catalog
├── build.gradle.kts         # Root build configuration
└── settings.gradle.kts      # Project settings
```

### Feature Organization (within `composeApp/src/commonMain/kotlin/io/github/alexmaryin/followmymus/`)

- `rootNavigation/` - App navigation and root component
- `screens/` - UI screens (splash, login, signUp, mainScreen with sub-pages)
- `musicBrainz/` - MusicBrainz API integration (data/domain layers)
- `supabase/` - Supabase backend integration
- `core/` - Shared utilities (DI modules, UI theme, preferences, result handling)
- `preferences/` - DataStore-based preferences management

## Building and Running

### Prerequisites
- **JDK 21** (configured via `jvmToolchain(21)`)
- **Android SDK** (compileSdk 36, minSdk 24, targetSdk 36)
- **Xcode** (for iOS builds on macOS)
- **Gradle 8.x** (wrapper included)

### Build Commands

#### Android Application
```bash
# Debug build
./gradlew :composeApp:assembleDebug

# Release build
./gradlew :composeApp:assembleRelease

# Install on connected device
./gradlew :androidApp:installDebug
```

#### Desktop (JVM) Application
```bash
# Run development version
./gradlew :composeApp:run

# Build distributable
./gradlew :composeApp:packageDistributionForCurrentOS
```

#### iOS Application
```bash
# Build for iOS simulator
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Or open in Xcode
open iosApp/iosApp.xcodeproj
```

### Test Commands
```bash
# Run all tests
./gradlew allTests

# Run JVM tests
./gradlew :composeApp:jvmTest

# Run Android tests (requires device/emulator)
./gradlew :composeApp:connectedAndroidTest
```

### Configuration
The project uses **BuildKonfig** for build-time configuration. Create a `local.properties` file in the project root with:
```properties
projectId=<your-supabase-project-id>
publishableKey=<your-supabase-publishable-key>
secretKey=<your-supabase-secret-key>
```

## Development Conventions

### Code Style
- **Kotlin coding conventions**: Follow official Kotlin style guide
- **Compose naming**: UI components use composable function naming conventions
- **Package structure**: Feature-based organization with `ui/domain/data` separation

### Dependency Injection
- Koin annotations with KSP code generation
- DI modules located in `core/di/` package
- Use `@Module`, `@Single`, `@Factory`, `@Scoped` annotations

### Testing Practices
- **Unit tests**: Located in `commonTest/` and `jvmTest/`
- **Mocking**: MockK library for mocking dependencies
- **Compose UI tests**: Using `compose.ui.test` APIs

### Key Patterns
- **MVI-like**: Components expose `state` and `action` handlers
- **Decompose**: Child components for navigation and feature isolation
- **Repository pattern**: Data access abstracted through repositories
- **Sealed interfaces**: For state and action modeling

## Platform-Specific Notes

### Android
- Uses CameraX and ML Kit for QR code scanning
- Kotzilla Analytics integrated for usage tracking
- Activity-based entry point in `androidApp` module

### iOS
- Framework exported for iOS targets (arm64, simulator)
- Decompose and Essenty lifecycle exported as frameworks
- SwiftUI entry point in `iosApp/iosApp/`

### Desktop (JVM)
- ZXing library for QR code functionality
- Swing integration via `kotlinx-coroutines-swing`
- Packaged as DMG, MSI, or DEB distributions

## Useful Links
- [Kotlin Multiplatform Docs](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Decompose Navigation](https://arkivanov.github.io/Decompose/)
- [Koin Documentation](https://insert-koin.io/)
- [Supabase Kotlin SDK](https://supabase.com/docs/reference/kotlin)
