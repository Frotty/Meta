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
- Reactive state core: `runtime/src/main/kotlin/de/fatox/meta/reactive/Reactive.kt`; widget bindings: `ui/MetaBind.kt`
- UI toolkit: `runtime/src/main/kotlin/de/fatox/meta/ui/components/` (TTF widgets), `ui/MetaUi.kt` (design tokens),
  `ui/layout/MetaLayout.kt` (layout checks)

## UI toolkit — batteries included, TTF everywhere
Meta is a batteries-included UI layer built directly on libGDX scene2d. VisUI is EOL and must not be reintroduced.
Follow these so screens share one look:
- **Use the TTF Meta widgets, not raw scene2d text widgets.** All text must render through the Meta font provider:
  `MetaLabel`, `MetaTextButton`, `MetaIconTextButton`, `MetaSelectBox`, `MetaTextField`, `MetaCheckBox`. Do NOT use
  raw `Label`/`TextButton`/`TextField` for visible text. If a needed widget still uses a baked font, add a
  `Meta*` wrapper that swaps in `fontProvider.getFont(size, type)` (see `MetaTextField` for the pattern: clone the
  style once, never mutate the shared skin style).
- **Use icon-style controls intentionally.** `MetaIconButton` is the full action icon button (same visual family as
  normal buttons). For a plain icon that is clickable but should stay visually lighter/subtle, use `MetaImageButton`.
- **Use Remix font icons for UI glyphs.** Prefer `MetaIcon("ri-information-line")`,
  `MetaImageButton("ri-add-line")`, and `MetaIconTextButton("Open", "ri-folder-open-line")` over one-off PNG
  toolbar icons. The bundled catalog is `assets/ui/icons/remixicon.tsv` (also shipped in runtime resources as
  `ui/icons/remixicon.tsv`); search it for supported names/categories instead of adding a giant enum. Do not add
  singular texture assets for ordinary UI glyphs. Bitmap textures are for actual game/editor art, logos, previews,
  screenshots, skin atlases, or generated drawables that cannot be expressed as a Remix icon.
- **Pointer cursors are automatic on Meta buttons.** The shared `cursorPointer()` behavior resolves the nearest
  enabled pointer actor, so nested controls do not fight over Hand/Arrow. Use it for custom clickable actors instead
  of installing raw cursor enter/exit listeners.
- **Tooltips may be attached at construction time.** `actor.tooltip("...")` remains registered while an actor is
  off-stage or temporarily detached in a cached window, and works again after re-attachment. Registrations use weak
  actor ownership, so abandoned actors remain collectible; call `removeTooltip()` only for explicit early removal.
- **Use `MetaIconButtonGroup` for icon tool palettes.** Brush/tool pickers should mark the active tool with
  `MetaIconButton.selected` / `MetaIconButtonGroup`, not scene2d checked state or Meta keyboard focus. Regular
  icon buttons stay momentary by default; the selected border is a visual-only active marker. For brush palettes,
  stay 100% Remix: use names like `ri-brush-line`, `ri-pencil-line`, `ri-paint-line`, `ri-magic-line`,
  `ri-eraser-line`, `ri-square-fill`/`ri-square-line`, and `ri-circle-fill`/`ri-circle-line` instead of custom
  drawn glyphs.
- **Design tokens live in `ui/MetaUi.kt`** — `MetaType` (typographic scale in px: CAPTION…DISPLAY), `MetaSpacing`
  (padding rhythm), `MetaColor` (dark palette). Prefer these over magic numbers/colors. Helpers: `metaLabel(...)`,
  `metaButton(...)`, `metaRow { ... }`, `metaColumn { ... }`, `Table.metaDefaults()`. Default text controls use
  `MetaType.BODY`; pass a different token only when the hierarchy calls for it. `MetaTable` stays neutral because it
  is also the structural layout primitive: use `MetaTable(defaultSpacing = true)` or the row/column helpers for normal
  content rather than repeating cell gaps manually. `MetaColor` values are shared mutable `Color`s — treat read-only,
  use `.cpy()` for variants.
- **Take the composed-control path first.** For forms use `MetaInputLayout.field(...)` / `.area(...)`; these provide
  label, field sizing, helper/error presentation, and collapse unused feedback space. `MetaIconTextButton` lays out a
  normal horizontal icon + label action by default; use `vertical = true` only for deliberate tile/grid controls.
  `SliderWithButtons` already supplies consistently sized step actions. Extend these reusable defaults when a common
  composition is missing instead of rebuilding it in each screen.
- **Use `MetaScrollPane`, not raw `ScrollPane`.** It owns Meta's thin generated scrollbar style,
  mouse-wheel step, right-side content gutter, and automatic hover-based scroll focus. Nested panes claim focus on
  mouse enter and restore the containing pane on exit, so consumers must not add their own scroll-focus listeners. If
  you need a scrollable list, wrap it in `MetaScrollPane` and let the component enforce the behavior and padding.
- **Use `MetaBottomBar` for bottom prompt/status strips.** It is a generic, content-width container with rounded top
  corners and reactive visibility/content binding helpers; keep game-specific glyph/font lookup in the consuming game
  and pass the resulting actor as content.
- **Window chrome and dialog actions are automatic.** `MetaWindow` has a title header and separator by default.
  Untitled `MetaDialog`/`MetaConfirmDialog` presentations collapse that header completely. Add dialog actions through
  `addButton`; the shared action row owns its border padding, so consumers must not compensate with one-off edge pads.
- **Verify layouts with `ui/layout/MetaLayout`** instead of only eyeballing the running game. `MetaLayout.problems(root)`
  / `assertValid(root)` find overflow (child outside parent) and clipping (actor smaller than its preferred size).
  It is pure geometry (no GL), so it runs in plain unit tests: bootstrap with `GdxTestEnvironment.ensure()` (test
  helper; `Table`/`Cell` need `Gdx.files`), build the layout with fixed-size stand-in widgets, `pack()`, assert.
  See `runtime/src/test/.../ui/layout/MetaLayoutTest.kt`.
- **Performance:** UI widgets run every frame — no per-frame allocations (no `String.format`, no new lambdas/lists
  in `draw`/`act`/`layout`). Cache and rebuild only on change (see `FPSGraph` for the reused-`StringBuilder` pattern).

## Input & modal-dialog contracts — always clean up global grabs
A whole class of "the dialog opens but its buttons are dead (no visible cause) and it stays broken" bugs comes from
**leaked global input state**. Anything that grabs input globally is a contract you MUST release on EVERY exit path.
- **`MetaInputProcessor.exclusiveProcessor` is a stack.** While non-null it routes ALL input to the top owner and
  bypasses the scene2d stage — so a leaked one makes every later button silently unresponsive. Use
  `pushExclusiveProcessor(p)` / `popExclusiveProcessor(p)` (the `var` setter is a convenience: non-null = push,
  null = pop top). Popping restores the previous owner, so nested grabs work.
- **Release in `MetaDialog.onHidden()`, not just your success handler.** `onHidden()` runs whenever the dialog
  leaves the stage by ANY path (button, ESC, click-away, screen change, programmatic). Clearing an exclusive grab
  (or a global/controller listener, or a focus override) only inside your keyDown/onClick handler leaks it whenever
  the dialog closes some other way. See `MetaKeyRebindDialog` for the pattern.
- A lingering `exclusiveProcessor` at the moment a normal modal is shown is itself a **bug signal** (missing pop):
  `UIManager.showDialog` logs a warning and resets it so input survives — but fix the leak at its source.
- Same rule for any other global contract you set when showing UI (stage capture listeners, controller listeners,
  `UiControlHelper` flags, `Gdx.input.inputProcessor`): set it on show, undo it on hide. Don't rely on the happy path.
- `UIManager.showDialog` cancels existing scene2d touch focus before the modal takes over, preventing a press/drag
  begun behind the dialog from completing through it. Normal clicks outside text inputs also clear stale keyboard
  focus automatically; do not duplicate either behavior in consumers.

## Reactive state — USE THIS, don't reinvent it
Meta has ONE ground-truth reactivity system (`de.fatox.meta.reactive`). Prefer it over ad-hoc
observer lists, manual "re-query and rebuild" code, or bespoke listener interfaces.
- **Primitives:** `signal(initial)` (writable cell), `computed { }` (lazy/memoized derived value),
  `effect { }` (auto-tracked side effect), `batch { }` (coalesce writes), `untracked { }`,
  `value.subscribe { }` (listener shape), `onCleanup { }`.
- **Do NOT** add new `MetaNotifier`-style classes or hand-rolled `addListener/notifyListeners`
  pairs. That pattern was removed; model the state as a `signal`/`computed` and let consumers
  `effect`/`subscribe`. UI that mirrors state should bind via `ui/MetaBind.kt` — these work with the Meta TTF
  widgets (`metaLabel.bindText { }`, `metaButton.bindText { }`) as well as generic scene2d (`actor.bindVisible { }`,
  `actor.bindColor { }`, `disableable.bindDisabled { }`). Own each binding in a `ReactiveScope` (scope-owned
  overloads like `scope.bindText(label) { }`) and dispose the scope on teardown. Use a binding when one widget
  property tracks one piece of state; use `signal.subscribe { rebuildSection() }` for coarse "rebuild on change".
- **Lifecycle: prefer the auto-disposing window scope.** Every `MetaWindow`/`MetaDialog` exposes a
  `reactiveScope` that is opened on show and disposed on hide. Create per-presentation bindings inside the
  `onShown()` hook (e.g. `reactiveScope.bindText(label) { someSignal() }`) and they tear down automatically on
  `onRemovedFromStage()` — the window can be shown/hidden/re-shown without leaking. Do NOT bind in `init`.
  For other transient owners (a `Screen`, a non-window view) make your own `ReactiveScope` and `dispose()` it on
  teardown. App-lifetime effects on a DI singleton may simply never be disposed. An undisposed `effect` keeps the
  actors it captured alive — scene2d has no GC-driven "removed" callback, so disposal is on you.
- **Threading:** drive it from the GL/render thread only (it is not thread-safe). Dispatch async
  callbacks onto that thread before writing signals.
- **Cycle safety:** a feedback loop between effects is capped per flush
  (`maxEffectRunsPerFlush`) and throws `ReactiveCycleException` instead of freezing — name your
  effects (`effect("myThing") { }`) so the culprit is identifiable. Catch it at your top-level loop
  to recover gracefully.
- Tests/spec live in `runtime/src/test/kotlin/de/fatox/meta/reactive/ReactiveTest.kt` — read them
  for exact semantics, and add cases when you extend the core.

## Performance posture (game-engine focused)
This is a **garbage-collected (JVM)** runtime rendering every frame. The dominant, *controllable* performance lever
here is **allocation rate** — minimize GC churn; most other micro-optimizations are not worth it (see below).
- **No per-frame allocation in hot paths** (`render`/`act`/`draw`/`layout`/input handlers, and UI redraw). That means:
  no `String.format`/string concatenation, no `+`/`map`/`filter`/`forEach` that boxes or allocates iterators/lambdas,
  no `new` temp objects. Build strings into a reused `StringBuilder` and rebuild only when a displayed value changes
  (see `FPSGraph`). Reuse mutable temps (`Vector2`, `Color`, layout scratch) — keep them as fields, not locals.
- **Use libGDX-native collections** (`Array`, `ObjectMap`, `IntMap`, `Pool`, `CharArray`) over the Kotlin/Java stdlib
  in hot code: they avoid boxing and let you control growth. `Pool` mutable objects you'd otherwise allocate per frame.
- **libGDX collection iterator quirk:** never iterate a libGDX `Array` with `for (value in array)`, `forEach`, or an
  explicit iterator. libGDX reuses its `Array` iterators, so nested iteration over the same array can invalidate the
  outer iterator and fail at runtime. Use an indexed loop (`for (i in 0 until array.size) array[i]`) whenever random
  access is available; it is nesting-safe and avoids iterator overhead. Do not suppress `GDXKotlinUnsafeIterator` to
  permit iterator-based loops.
- **Beware hidden allocations:** Kotlin lambdas that capture, autoboxing of `Int`/`Float` into generic collections,
  varargs, and iterator-based loops over other collection types.
- **Don't chase what the JVM hides from you.** Cache-line layout, struct packing, manual SIMD, branch-elimination etc.
  are largely out of reach on the JVM (objects are boxed/scattered, the JIT decides) — spending effort there is mostly
  wasted. Spend it on: fewer allocations, fewer draw calls / batches, fewer scene2d invalidations, and algorithmic wins.
- The reactive core is allocation-light by design (effects re-run only on real change); still, don't create signals/
  effects per frame — wire them once.

## Platform & dependency notes (libGDX / LWJGL3 / Kotlin)
- **Desktop backend is LWJGL3** and carries real quirks — prefer Meta wrappers over poking GLFW directly, and when you
  must, document the workaround:
  - **Borderless / fullscreen:** use libGDX `Graphics` APIs (`setFullscreenMode`/`setWindowedMode`); borderless is a
    windowed mode sized to the monitor, not true exclusive fullscreen. Mode switches can drop the GL context — reload
    GL-resident resources (textures/FBOs/fonts) and re-layout on resize. Persist window size/pos and restore on launch.
  - **Multi-monitor / DPI:** monitor coordinates are virtual-desktop relative and HiDPI scaling differs per OS — never
    hardcode pixel sizes; drive layout off `Graphics` viewport values (the `ScreenViewport` already does).
  - **HiDPI UI scaling is automatic.** `UIRenderer` seeds a global `uiScale` from the display (`suggestedUiScale()`)
    so controls aren't tiny on 4K/Retina — every consumer gets it for free. It's a reactive `Signal<Float>`: a game's
    settings slider can bind to `uiRenderer.uiScale` (persist the user's choice and set it on boot) and the whole UI
    re-scales live. Because of this, size widgets in UI units (not raw pixels) and never read `Gdx.graphics` pixels for
    layout.
  - **Startup:** a separate splash/loading window before the GL window can race the main window/context — gate UI work
    on the GL thread and on assets actually being loaded. Heavy work off the GL thread must hop back via
    `Gdx.app.postRunnable` before touching GL/scene2d.
  - **Input:** route through `MetaInputProcessor` (exclusive-grab stack, key/scroll listeners); don't set
    `Gdx.input.inputProcessor` directly or you'll bypass Meta's contracts.
- **Kotlin / libktx stance:** Meta is an *alternative* to libktx, not a companion — don't add the libktx dependency.
  Its modules overlap Meta's own opinionated layer (ktx-scene2d vs the Meta widgets/`MetaBind`, ktx-async vs the
  reactive core, KTX DI vs `MetaInject`), and pulling it in gives agents two ways to do everything (worse
  digestibility) plus a dependency we don't control. If a specific, stable libktx utility is genuinely missing here,
  port the small piece into the right `de.fatox.meta.*` package rather than taking the whole library.

## What belongs here vs. in a consuming game
Goal: consumer repos (e.g. OxRox) hold *game* code; anything generic and reusable across games lives in Meta.
- **Belongs in Meta:** generic scene2d widgets, input/key utilities, deterministic RNG, generic
  data structures, serialization/encoding helpers, reusable dialogs, reactive bindings — anything with no
  game-specific logic. A strong tell: it already sits in a `de.fatox.meta.*` package, or depends only on
  Meta + libGDX scene2d.
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
  - Kotlin `2.4.0`
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
