# Failing Tests — RootTest

## Root Cause

Both failing tests (`Check if app navigates to Login if not authenticated` and `Check if app navigates to Main if authenticated`) crash with the same error:

```
org.koin.core.error.NoDefinitionFoundException:
No definition found for type 'io.github.alexmaryin.followmymus.preferences.PreferenceSource'
```

`RootContent` composes `PreferencesHandler` which calls `koinInject()` to resolve `PreferenceSource`, but the test Koin module in `RootTest.setUp()` does not register it.

## The Fix

Add a `PreferenceSource` definition to the test module in `RootTest.kt` (`RootTest.setUp()`, line ~109-126):

```kotlin
startKoin {
    modules(
        module {
            // ... existing mocks ...
            factory<PreferenceSource> { PreferenceSource(mockk()) }
        }
    )
}
```

The import needed:
```kotlin
import io.github.alexmaryin.followmymus.preferences.PreferenceSource
```

## Status

- Tests are non-functional on the JVM as of the last verified run.
- Both tests **do** compile — they fail at runtime during Composable layout.
- All other 53 tests (including `Check if app splash starts with Logo`, which does not use `RootContent`) pass.
- The `PreferenceSource` is provided in production by the `AppModule` in `apiModule.kt`, but the test module only registers mocks for a few dependencies and omits this one.

## Related Files

| File | Role |
|---|---|
| `composeApp/src/jvmTest/.../RootTest.kt` | Test class — missing `PreferenceSource` in Koin setup |
| `composeApp/src/commonMain/.../preferences/PreferenceSource.kt` | The class that cannot be resolved |
| `composeApp/src/commonMain/.../rootNavigation/ui/PreferencesHandler.kt` | Composable that triggers the injection |
| `composeApp/src/commonMain/.../core/di/apiModule.kt` | Production Koin module that registers it |
