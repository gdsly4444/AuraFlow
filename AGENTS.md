# Aura — Agent Guidelines

**Aura** is the Android app (`com.catclaw.aura`). Gradle root project name: **Aura**.

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
  AuraApplication.kt            # NetworkClient.init()
  MainActivity.kt               # Fragment container + replaceFragment() helpers
  data/
    network/                    # Retrofit + OkHttp (NetworkClient)
    local/                      # Room (entities TBD)
  ui/
    base/
    map/
    <feature>/
```

Add a `showXxxFragment()` helper on MainActivity and call `replaceFragment` from there.

## Network

Init in `AuraApplication`. Usage: [app/docs/network.md](./app/docs/network.md) (`NetworkClient.get` / `postJson` / `postForm` + `HttpCallback`).

## Build and test

Use the **system Gradle cache** (`~/.gradle`). Do not trigger wrapper downloads in CI/sandbox; if `gradle-9.4.1-bin` is already under `~/.gradle/wrapper/dists`, run offline:

```bash
GRADLE_USER_HOME="$HOME/.gradle" ./gradlew --offline assembleDebug
GRADLE_USER_HOME="$HOME/.gradle" ./gradlew --offline test
```

Or use `./scripts/gradlew-local.sh assembleDebug` (sets `GRADLE_USER_HOME` and `--offline` by default).

Mapbox token: `MAPBOX_ACCESS_TOKEN` in `local.properties` (injected via `resValue`).

## Conventions

- ViewModels expose `StateFlow<UiState>` and `SharedFlow<UiEvent>` via `BaseViewModel`.
- Fragments extend `BaseFragment`, use `by viewModels()`, collect state in `onBind`.
- Prefer version catalog entries for new dependencies.
- Do not commit secrets.
