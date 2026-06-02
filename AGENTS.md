# AuraFlow — Agent Guidelines

AuraFlow is an Android app (`com.catclaw.aura`) in early scaffold. The Gradle root project name is **Aura**; the repo is **AuraFlow**.

## Stack

- **Language**: Kotlin (no Compose yet — XML layouts + `AppCompatActivity`)
- **Build**: Gradle 9.4.1, AGP 9.2.1, version catalog (`gradle/libs.versions.toml`)
- **SDK**: `minSdk` 35, `targetSdk` 36, `compileSdk` 36 (minor API 1)
- **UI**: Material, ConstraintLayout, edge-to-edge insets in `MainActivity`

## Layout

```
app/
  src/main/java/com/catclaw/aura/   # Application code
  src/main/res/                     # Layouts, themes, drawables
  src/test/                         # JVM unit tests
  src/androidTest/                  # Instrumented tests
gradle/libs.versions.toml           # Dependency versions
```

Single module: `:app` (`settings.gradle`).

## Build and test

```bash
./gradlew assembleDebug          # Debug APK
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented (device/emulator required)
```

`local.properties` (Android SDK path) is gitignored; required for local/CI builds.

## Conventions

- Keep changes scoped; match existing View-system patterns until a deliberate Compose migration.
- Prefer version catalog entries in `libs.versions.toml` for new dependencies.
- Package namespace: `com.catclaw.aura`.
- Do not commit secrets (`*.jks`, `google-services.json`, API keys).

## Git state (as of init)

Android scaffold may be staged but uncommitted on `main`. Only commit when the user explicitly asks.
