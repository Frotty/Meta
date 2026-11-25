# AGENTS.md

## Project and scope

Meta is a shared Kotlin/libGDX engine layer used by games including OxRox. Compatibility and runtime stability take
priority over local convenience.

- Default to `runtime/` and `runtime-desktop/`; the editor is an optional consumer unless explicitly in scope.
- Put generic, reusable engine behavior in Meta. Keep levels, gameplay, online services, art conventions, and other
  game-specific behavior in the consuming game.
- Preserve unrelated working-tree changes. Do not patch generated outputs when an authoritative source exists.

### What counts as a defect

Weigh a finding by whether intended usage reaches it. A game saves settings, a player alt-F4s, the game reads them
back next launch; a kill mid-write counts, and so does a file a scanner or sync client holds for a moment.

These are **not** trust boundaries, and hardening them is out of scope unless asked:

- Save data and project metadata. One process writes and reads them, in a directory the user already owns. A local
  attacker with write access there can rewrite the files directly, so guarding against planted symlinks, races on
  our own scratch files, or crafted contents buys nothing.
- XPK archives. The aim is that a media-extraction tool sweeping for magic bytes finds nothing, not that a
  determined person cannot get the assets - the decoder ships in the jar. See `docs/xpk-format-audit.md` §8.

Reviewers, human or automated, will raise adversarial-input findings against this code anyway. Say which of these
applies and move on; do not harden by reflex, and do not add an unused escape hatch to a guard that has one job.

| Module | Ownership |
| --- | --- |
| `runtime` | Core runtime: UI, assets, DI, input, audio, persistence, reactive state. |
| `runtime-desktop` | LWJGL3 platform bindings. |
| `editor` | Optional scene/shader/editor features built on runtime. |
| `editor-desktop` | Optional editor launcher and UI playground. |

Key entry points below are relative to `runtime/src/main/kotlin/de/fatox/meta/`:

- Framework and defaults: `Meta.kt`, `MetaModule.kt`
- DI: `injection/MetaInject.kt`
- UI contracts: `api/ui/UIManager.kt`, `ui/MetaUiManager.kt`, `ui/MetaUIRenderer.kt`
- UI tokens and layout: `ui/MetaUi.kt`, `ui/layout/MetaLayout.kt`, `ui/components/`
- Reactive state and bindings: `reactive/Reactive.kt`, `ui/MetaBind.kt`, `api/ReactiveScreenAdapter.kt`
- Loading and assets: `api/SplashScreen.kt`, `api/AssetProvider.kt`, `assets/MetaAssetProvider.kt`
- Persistence and history: `assets/MetaData.kt`, `task/MetaTaskManager.kt`

## UI contracts

Meta is the scene2d UI layer; VisUI and libktx must not be introduced.

- Visible text uses TTF-backed Meta widgets: `MetaLabel`, `MetaTextButton`, `MetaIconTextButton`, `MetaSelectBox`,
  `MetaTextField`, `MetaTextArea`, and `MetaCheckBox`. A new text widget must clone its skin style before replacing
  the font; never mutate a shared style.
- Widgets that cache font data must track the provider that supplies it and refresh stale fonts before reading
  their metrics. Layout can measure a detached widget before it has a chance to draw.
- Use `MetaIconButton` for full action affordance and `MetaImageButton` for a lighter icon-only action. Choose
  `MetaButtonTier.PRIMARY`, `SECONDARY`, or `TERTIARY` by semantic emphasis instead of local colors.
- Routine glyphs come from the bundled Remix catalog (`assets/ui/icons/remixicon.tsv`). Bitmap assets are for art,
  logos, previews, screenshots, atlases, or visuals that cannot be expressed by the icon font.
- Meta buttons own pointer cursors. Use `cursorPointer()` only for custom clickable actors. Construction-time
  tooltips remain registered while actors are detached; remove them only for explicit early cleanup.
- Tool palettes use `MetaIconButtonGroup` and `selected`, not checked state or keyboard focus.
- Prefer `MetaFlexBox` for rows, columns, forms, toolbars, and wrapping; `MetaGrid` for shared equal tracks;
  `MetaStack` for overlays. Keep `MetaTable` for genuine scene2d cell/row-span semantics. Layout measurement must be
  allocation-free and must not rebuild rows from `sizeChanged()`.
- Use tokens from `MetaUi.kt`: `MetaType`, `MetaSpacing`, `MetaColor`, and `MetaButtonTier`. Treat shared colors as
  read-only and call `.cpy()` for variants.
- Button prompts use `MetaPromptBar` / `MetaInputPrompt` with Kenney glyphs from `MetaInputGlyphs` (face buttons by
  position, triggers, d-pad, menu, `key(keyCode, fallbackText)`). The art is Kenney's and stays Kenney's: do not
  replace it with drawn shapes. Glyph names must exist in `ui/prompts/<face>.tsv`; `MetaInputPromptTest` checks the
  presets. If prompts appear on the first frame, warm `MetaInputPrompt.requiredFonts(size)` and
  `MetaInputPrompt.prewarm(glyphs, size, ppu)` at startup - otherwise the first draw opens and rasterizes the face.
- Prefer composed controls: `MetaInputLayout`, `MetaIconTextButton`, `SliderWithButtons`, `MetaActionList`,
  `MetaActionRow`, and `MetaBottomBar`.
- Use `MetaScrollPane`; it owns scrollbar styling, content gutter, wheel step, and nested hover focus.
- `MetaWindow`/`MetaDialog` own chrome, sticky controls/actions, responsive overflow, and constrained scrolling.
  Consumers must not wrap the whole window in another scroll pane. `MetaUiManager` alone owns docking layout and
  persistence.
- Test geometry with `MetaLayout.problems(root)` or `MetaLayout.assertValid(root)`. Use `GdxTestEnvironment.ensure()`
  for plain actors and `MetaHeadlessUi.install()` to measure real widget and screen trees with GL stubs. Draw calls
  are discarded. Use the playground to verify pixels. See the [fixture setup](README.md#develop-and-verify).

## Global input and lifecycle cleanup

Any global input or visibility grab must be released on every exit path.

- Use `MetaInputProcessor.pushExclusiveProcessor`/`popExclusiveProcessor` to manage the exclusive input stack.
  A leaked owner bypasses the stage and makes later UI unresponsive.
- Release grabs, capture listeners, controller listeners, focus overrides, and helper flags in
  `MetaDialog.onHidden()`, not only in success/cancel handlers.
- Use `UIManager.temporarilyHideOtherWindows(owner)` and dispose its lease. Do not toggle every window manually.
- Scene2d keyboard focus is authoritative while editing a `TextField`/`TextArea`; navigation helpers must not consume
  editing keys.
- Route input through `MetaInputProcessor`. Do not replace `Gdx.input.inputProcessor`.

`UIManager.showDialog` already cancels stale touch focus, clears leaked exclusive input as a recovery measure, and
normal clicks clear stale keyboard focus. Fix leaks at their source instead of duplicating these safeguards.

## Reactive state

`de.fatox.meta.reactive` is the only application state mechanism. Use `signal`, `computed`, `effect`, `batch`,
`untracked`, `subscribe`, and `onCleanup`; do not add observer lists or notifier classes.

- Bind individual widget properties through `MetaBind.kt`; use a subscription for coarse section rebuilds.
- `MetaWindow`/`MetaDialog` bindings are created in `onShown()` with their renewable `reactiveScope`. Screens extend
  `ReactiveScreenAdapter`. Other transient views own and dispose a `ReactiveScope` explicitly.
- Widget `textValue`, `checkedValue`, `disabledValue`, and `selectedValue` signals are bidirectional; do not mirror
  them with another listener-backed state object.
- Bind undo/redo UI to `MetaTaskManager.canUndo` and `canRedo`.
- Reactive writes are GL-thread-only. Dispatch worker callbacks before changing UI-driving state.
- Name effects where practical. Cycles throw `ReactiveCycleException`; handle it at the top-level loop.

## Performance and collections

Allocation rate is the primary controllable JVM game-runtime cost.

- No per-frame strings, temporary objects, captured lambdas, boxing collections, varargs, or iterator allocation.
  `draw`, `act` and `layout` are the UI hot paths this governs.
  Reuse `StringBuilder`, vectors, colors, arrays, and other scratch state; rebuild only when values change.
- Prefer libGDX collections in hot code. Never iterate a libGDX `Array` with `for (value in array)`, `forEach`, or an
  iterator: its reusable iterators are not nesting-safe. Use indexed loops or the indexed `forEachValue` helpers.
- Lifecycle/setup traversal of `ObjectMap`, `IntMap`, or `LongMap` may use `forEachEntryReentrant`; it allocates and
  does not belong in hot paths.
- Prioritize allocation reduction, algorithmic wins, fewer draw calls, and fewer scene2d invalidations over JVM
  memory-layout or branch micro-optimizations.

`verifyKotlinIterationSafety` checks production Kotlin across all modules before Kotlin compilation and `check`.

## Platform, startup, and resources

- Desktop uses LWJGL3. Use libGDX graphics APIs for display changes and Meta platform wrappers elsewhere. Display
  changes can recreate GL resources; monitor coordinates and DPI are platform-dependent.
- UI scaling comes from `UIRenderer.uiScale`. Size in UI units. Commit dragged scale sliders through
  `SliderWithButtons.committedValue` so relayout does not move the active gesture.
- Prefer `SplashScreen(prepareAssets, queueAssets, onLoaded)` for non-trivial startup. The first two callbacks run
  sequentially on a worker and may do CPU/file/queue work only. `onLoaded`, scene2d, GL resources, and reactive UI
  writes run on the GL thread.
- GL-thread startup work of the application's own (its faces, atlases or a pre-built scene) belongs in
  `SplashCallbacks.startupLoad`, which the splash advances in budgeted slices while the panel animates. Anything left
  in `onLoaded` blocks a frame with the panel already gone, so keep that to handing over the first screen.
- Consuming applications extend `Meta` to initialize the runtime object graph.
- Reach platform handlers (`WindowHandler`, `MonitorHandler`, `SoundHandler`, `GraphicsHandler`) through the injection
  graph, never through `Meta.instance`. `Meta.create` registers the application's own; `MetaModule` supplies `No*`
  defaults, and `MetaHeadlessUi` registers them for tests. A static singleton is not resolvable in a unit test, which
  is why the display, FBO and sound paths had no coverage.
- Use `AssetProvider.load` with frame-budgeted updates. Avoid `finish()` and unqueued `getResource()` on animated
  loading paths.
- Consult the [assets and persistence guidance](runtime/src/main/kotlin/de/fatox/meta/assets/AGENTS.md) before
  changing either XPK format or the metadata store.
- Runtime resources must be generic and runtime-used. Keep the authoring and runtime copies of Remix font/catalog
  data byte-identical. Do not ship editor/sample content in `runtime/src/main/resources`.
- Do not build new code on deprecated placeholders such as `AssetPromise`, `MetaShortcut`, `MetaTaskQueue`,
  `TaskListener`, `BufferRenderer`, or `MetaListener`.

## Compatibility and verification

- Prefer additive APIs and deprecation before removal. Check downstream call sites for shared API changes.
- Preserve serialization keys and persisted-data compatibility; `PersistedFieldNamesTest` pins the field names of
  everything Meta persists, so a rename fails there rather than in someone's save. Keep desktop/editor launchers
  aligned with Gradle changes.
- Upgrade Meta and verify it before bumping a consumer's pinned `metaVersion`/commit.
- The runtime targets Java 25. See `gradle.properties` for dependency pins and the Gradle wrapper for the build version.
  Keep the Jenkins Gradle image in `ci/Dockerfile` aligned with the wrapper.

Minimum runtime gates:

```powershell
.\gradlew.bat :runtime:compileKotlin
.\gradlew.bat :runtime:test
```

For shared/public API changes:

```powershell
.\gradlew.bat :runtime-desktop:compileKotlin :editor:compileKotlin :editor-desktop:compileKotlin
```

For UI changes, also run `:editor-desktop:runMetaUiPlayground` when practical and add deterministic geometry or
behavior tests. Do not hand off API- or performance-sensitive changes without compile and test evidence.
