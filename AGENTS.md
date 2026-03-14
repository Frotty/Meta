# AGENTS.md

## Purpose
Meta is a Kotlin/libGDX engine layer used by downstream games (including OxRox).
Changes here affect multiple projects, so compatibility and runtime behavior stability are critical.

## Module map
- `runtime/`: core engine runtime (UI manager, assets, DI, input, audio, data).
- `runtime-desktop/`: desktop platform bindings for runtime.
- `editor/`: scene/shader/editor logic built on runtime.
- `editor-desktop/`: desktop launcher/package for editor.

## Core architecture
- App framework entrypoint/base: `runtime/src/main/kotlin/de/fatox/meta/Meta.kt`
- Dependency injection container: `runtime/src/main/kotlin/de/fatox/meta/injection/MetaInject.kt`
- UI/window registry APIs: `runtime/src/main/kotlin/de/fatox/meta/api/ui/UIManager.kt`
- Data persistence/metadata: `runtime/src/main/kotlin/de/fatox/meta/assets/MetaData.kt`

## Performance posture (game-engine focused)
- Prefer libGDX-native collections and buffers in hot paths (`Array`, `ObjectMap`, `IntMap`, `CharArray`, pools).
- Avoid avoidable allocations in render/update/input loops and UI redraw paths.
- Reuse mutable temp objects where safe; avoid per-frame lambdas/streams/temporary strings in critical loops.
- For text buffering in hot code, prefer libGDX `CharArray` over allocation-heavy patterns.

## Compatibility policy
- `runtime` is a shared dependency for client projects; avoid breaking public APIs without clear migration updates.
- Serialization/data key changes require backward compatibility checks.
- Keep desktop/editor launchers aligned with Gradle plugin API changes.

## Build/toolchain notes
- Gradle wrapper is authoritative for this repo.
- Toolchain auto-download is enabled; daemon JVM criteria file is tracked in `gradle/gradle-daemon-jvm.properties`.
- Current baseline in this repo:
  - libGDX `1.14.0`
  - Kotlin `2.3.10`
  - Gradle `9.1.0`

## Testing expectations
- At minimum, run:
  - `.\gradlew.bat :runtime:compileKotlin`
  - `.\gradlew.bat :runtime:test`
- For cross-module changes, run compile tasks for all modules.
- Do not merge API/perf-sensitive changes without compile+test verification.

## Upgrade workflow
- Upgrade Meta first (this repo), verify modules/tests, then bump consumers (e.g. OxRox `metaVersion`).
- When upgrading libGDX/Kotlin/Gradle:
  - expect API shifts (especially utility classes and build DSL)
  - patch engine code first
  - only then propagate to downstream projects
