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
- Reactive state core: `runtime/src/main/kotlin/de/fatox/meta/reactive/Reactive.kt` (+ `ReactiveScene2d.kt` bindings)
- UI toolkit: `runtime/src/main/kotlin/de/fatox/meta/ui/components/` (TTF widgets), `ui/MetaUi.kt` (design tokens),
  `ui/layout/MetaLayout.kt` (layout checks)

## UI toolkit — batteries included, TTF everywhere
Meta aims to be a batteries-included UI layer on top of VisUI/scene2d. Follow these so screens share one look:
- **Use the TTF Meta widgets, not VisUI's baked-glyph ones.** All text must render through the Meta font provider:
  `MetaLabel`, `MetaTextButton`, `MetaIconTextButton`, `MetaSelectBox`, `MetaTextField`, `MetaCheckBox`. Do NOT use
  raw `VisLabel`/`VisTextButton`/`VisTextField` for visible text. If a needed widget still uses skin glyphs, add a
  `Meta*` wrapper that swaps in `fontProvider.getFont(size, type)` (see `MetaTextField` for the pattern: clone the
  style once, never mutate the shared skin style).
- **Design tokens live in `ui/MetaUi.kt`** — `MetaType` (typographic scale in px: CAPTION…DISPLAY), `MetaSpacing`
  (padding rhythm), `MetaColor` (dark palette). Prefer these over magic numbers/colors. Helpers: `metaLabel(...)`,
  `metaButton(...)`, `Table.metaDefaults()`. `MetaColor` values are shared mutable `Color`s — treat read-only, use
  `.cpy()` for variants.
- **Verify layouts with `ui/layout/MetaLayout`** instead of only eyeballing the running game. `MetaLayout.problems(root)`
  / `assertValid(root)` find overflow (child outside parent) and clipping (actor smaller than its preferred size).
  It is pure geometry (no GL), so it runs in plain unit tests: bootstrap with `GdxTestEnvironment.ensure()` (test
  helper; `Table`/`Cell` need `Gdx.files`), build the layout with fixed-size stand-in widgets, `pack()`, assert.
  See `runtime/src/test/.../ui/layout/MetaLayoutTest.kt`.
- **Performance:** UI widgets run every frame — no per-frame allocations (no `String.format`, no new lambdas/lists
  in `draw`/`act`/`layout`). Cache and rebuild only on change (see `FPSGraph` for the reused-`StringBuilder` pattern).

## Reactive state — USE THIS, don't reinvent it
Meta has ONE ground-truth reactivity system (`de.fatox.meta.reactive`). Prefer it over ad-hoc
observer lists, manual "re-query and rebuild" code, or bespoke listener interfaces.
- **Primitives:** `signal(initial)` (writable cell), `computed { }` (lazy/memoized derived value),
  `effect { }` (auto-tracked side effect), `batch { }` (coalesce writes), `untracked { }`,
  `value.subscribe { }` (listener shape), `onCleanup { }`.
- **Do NOT** add new `MetaNotifier`-style classes or hand-rolled `addListener/notifyListeners`
  pairs. That pattern was removed; model the state as a `signal`/`computed` and let consumers
  `effect`/`subscribe`. UI that mirrors state should bind via `ReactiveScene2d.kt`
  (`label.bindText { }`, `actor.bindVisible { }`, `disableable.bindDisabled { }`).
- **Lifecycle is manual.** An `effect` lives until disposed and keeps captured actors alive.
  For anything tied to a transient owner (a `Screen`, a recreated window/view), create it through a
  `ReactiveScope` and `dispose()` that scope on teardown. App-lifetime effects on a DI singleton
  may simply never be disposed. scene2d has no "removed" callback — don't rely on GC.
- **Threading:** drive it from the GL/render thread only (it is not thread-safe). Dispatch async
  callbacks onto that thread before writing signals.
- **Cycle safety:** a feedback loop between effects is capped per flush
  (`maxEffectRunsPerFlush`) and throws `ReactiveCycleException` instead of freezing — name your
  effects (`effect("myThing") { }`) so the culprit is identifiable. Catch it at your top-level loop
  to recover gracefully.
- Tests/spec live in `runtime/src/test/kotlin/de/fatox/meta/reactive/ReactiveTest.kt` — read them
  for exact semantics, and add cases when you extend the core.

## Performance posture (game-engine focused)
- Prefer libGDX-native collections and buffers in hot paths (`Array`, `ObjectMap`, `IntMap`, `CharArray`, pools).
- Avoid avoidable allocations in render/update/input loops and UI redraw paths.
- Reuse mutable temp objects where safe; avoid per-frame lambdas/streams/temporary strings in critical loops.
- For text buffering in hot code, prefer libGDX `CharArray` over allocation-heavy patterns.

## What belongs here vs. in a consuming game
Goal: consumer repos (e.g. OxRox) hold *game* code; anything generic and reusable across games lives in Meta.
- **Belongs in Meta:** generic VisUI/scene2d widgets, input/key utilities, deterministic RNG, generic
  data structures, serialization/encoding helpers, reusable dialogs, reactive bindings — anything with no
  game-specific logic. A strong tell: it already sits in a `de.fatox.meta.*` package, or depends only on
  Meta + libGDX/VisUI.
- **Stays in the game:** levels, entities, gameplay rules, game-specific screens/content, Steam/online
  integration, art/asset conventions.
- When promoting code from a consumer into Meta: parameterize game-specific bits (e.g. asset paths via
  `AssetProvider`), add it under the right `de.fatox.meta.*` package, then remove it downstream and bump the
  consumer's `metaVersion`. Check for an existing Meta equivalent first (e.g. `api/encoding`, `api/crypto`)
  to avoid duplicates.

## Compatibility policy
- `runtime` is a shared dependency for client projects; avoid breaking public APIs without clear migration updates.
- Serialization/data key changes require backward compatibility checks.
- Keep desktop/editor launchers aligned with Gradle plugin API changes.
- Prefer the reactive core (see above) for new shared state instead of one-off observer mechanisms.

## Build/toolchain notes
- Gradle wrapper is authoritative for this repo.
- Toolchain auto-download is enabled; daemon JVM criteria file is tracked in `gradle/gradle-daemon-jvm.properties`.
- Current baseline in this repo:
  - libGDX `1.14.2`
  - VisUI `1.5.9`
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
