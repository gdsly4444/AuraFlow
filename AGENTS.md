# AuraFlow — Agent Guidelines

AuraFlow is an Android app (`com.catclaw.aura`). The Gradle root project name is **Aura**; the repo is **AuraFlow**.

## Stack

- **Language**: Kotlin
- **Architecture**: MVVM, single-activity + Fragment switching via `MainActivity`
- **Build**: Gradle 9.4.1, AGP 9.2.1, version catalog (`gradle/libs.versions.toml`)
- **SDK**: `minSdk` 35, `targetSdk` 36, `compileSdk` 36 (minor API 1)
- **UI**: XML layouts, ViewBinding, Material
- **Maps**: Mapbox Maps SDK 11.x (`MapView` in `MapFragment`)

## Package layout

```
com.catclaw.aura/
  MainActivity.kt                 # Fragment container + replaceFragment() helpers
  ui/
    base/
      BaseFragment.kt             # lifecycle-aware Flow collection
      BaseViewModel.kt            # StateFlow + SharedFlow helpers
    map/                          # one package per screen
      MapFragment.kt
      MapViewModel.kt
      MapUiState.kt
      MapUiEvent.kt
    <feature>/                    # add new screens here
      FeatureFragment.kt
      FeatureViewModel.kt
      FeatureUiState.kt
      FeatureUiEvent.kt
```

Add a `showXxxFragment()` helper on [MainActivity] and call [replaceFragment] from there.

## Build and test

```bash
./gradlew assembleDebug
./gradlew test
```

Mapbox token: `MAPBOX_ACCESS_TOKEN` in `local.properties` (injected via `resValue`).

## Conventions

- ViewModels expose `StateFlow<UiState>` and `SharedFlow<UiEvent>` via `BaseViewModel`.
- Fragments extend `BaseFragment`, use `by viewModels()`, collect state in `onBind`.
- Prefer version catalog entries for new dependencies.
- Do not commit secrets.
