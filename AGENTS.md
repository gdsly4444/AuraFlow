# Aura — Agent Guidelines

**Aura** is the Android app (`com.catclaw.aura`). Gradle root project name: **Aura**.

## Stack

- **Language**: Kotlin
- **Architecture**: MVVM, single-activity + Fragment switching via `MainActivity`
- **Build**: Gradle 9.4.1, AGP 9.2.1, version catalog (`gradle/libs.versions.toml`)
- **SDK**: `minSdk` 35, `targetSdk` 36, `compileSdk` 36
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

Use the **system Gradle cache** (`~/.gradle`). First clone / fresh machine: run **online**:

```bash
./gradlew assembleDebug
```

Optional offline (only after dependencies are cached): `AURA_GRADLE_OFFLINE=1 ./scripts/gradlew-local.sh assembleDebug`

Mapbox: `MAPBOX_ACCESS_TOKEN` (pk., app maps) and `MAPBOX_DOWNLOADS_TOKEN` (sk. with Downloads:Read, Gradle Maven) in `local.properties`.

DashScope (百炼 scene description): `DASHSCOPE_API_KEY` in `local.properties` → `BuildConfig.DASHSCOPE_API_KEY`. Never commit real keys; rotate if exposed. See `docs/scenedescription-plan.md` and `data/scenedescription/`.

Moment cards: sampling auto-starts `MomentWorkflowService` (up to 5 parallel workflows, unique `workflowId`). Cards persist in Room; home `MapFragment` shows a half-screen BottomSheet list; detail in `MomentDetailFragment`. See `docs/moment-cards-home-plan.md`.

## Conventions

- ViewModels expose `StateFlow<UiState>` and `SharedFlow<UiEvent>` via `BaseViewModel`.
- Fragments extend `BaseFragment`, use `by viewModels()`, collect state in `onBind`.
- Prefer version catalog entries for new dependencies.
- Do not commit secrets.
