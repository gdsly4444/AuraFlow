# AuraFlow

Android app for Aura flow (`com.catclaw.aura`).

## Requirements

- Android SDK (API 36 toolchain)
- JDK 11+ (project uses Java 11 compatibility)
- Device or emulator with API 35+ for install/run

## Quick start

1. Copy `local.properties.example` to `local.properties` (or add to your existing file).
2. Set `MAPBOX_ACCESS_TOKEN` to your Mapbox public token (`pk.`…).

```bash
./gradlew assembleDebug
```

Install the debug APK from `app/build/outputs/apk/debug/` on a connected device, or open the project in Android Studio and run **app**.

## Project structure

| Path | Purpose |
|------|---------|
| `app/src/main/java/com/catclaw/aura/` | Kotlin sources |
| `app/src/main/res/` | Resources (layouts, themes) |
| `gradle/libs.versions.toml` | Dependency versions |

See [AGENTS.md](./AGENTS.md) for conventions and commands used by coding agents.

## Mapbox 集成文档

中文译本见 [docs/mapbox/](./docs/mapbox/README.md)（基于 [Mapbox Android Maps Guides](https://docs.mapbox.com/android/maps/guides/)）。
