---
name: meta-best-practices
description: Audit or review Meta engine and Meta-consumer Kotlin/libGDX code for Meta reactive-state usage, Meta UI composition and responsiveness, Scene2D lifecycle, and crisp rendering. Use for Meta-specific architecture reviews, PR audits, or implementation checks; do not trigger for unrelated Kotlin/libGDX projects.
---

# Meta Best Practices

Audit Meta and its consumers against the framework contracts below. Report concrete, evidenced problems; do not demand rewrites for stylistic preference.

## Establish the applicable Meta API

- Determine whether the target is Meta itself or a consumer.
- In a consumer, inspect its pinned Meta version or commit before recommending APIs. Do not assume the consumer has APIs added after that revision.
- Prefer generic fixes in Meta when the behavior is recurrent across games. Keep gameplay, art conventions, online services, and other game-specific policy in the consumer.
- When exact semantics matter, inspect the target revision of `Reactive.kt`, `MetaBind.kt`, the relevant Meta UI component, and its tests.

## Reactive architecture is the default

Use `de.fatox.meta.reactive` as the application-state and change-propagation mechanism:

- `signal` owns mutable feature state.
- `computed` expresses pure derived state.
- `effect` performs lifecycle-bound side effects.
- `batch`, `untracked`, `subscribe`, `onCleanup`, and `ReactiveScope` handle transaction, dependency, interop, and lifetime concerns.

Flag custom observer lists, listener registries, callback fan-out, observable/property wrappers, event buses, polling-based UI synchronization, or parallel state stores that duplicate this model. This applies in Meta and in consumers.

Framework callbacks are allowed at system boundaries—for example Scene2D input listeners, HTTP completion callbacks, platform callbacks, and screen lifecycle methods. Treat them as adapters: translate the event into canonical reactive state on the GL thread, then let computed values and scoped effects propagate it. Do not grow another observer architecture behind the adapter.

Specific expectations:

- Keep one canonical signal instead of mirroring the same state through fields and listeners.
- Keep `computed` functions pure; do not write signals, perform I/O, mutate UI, or hide cleanup inside them.
- Scope effects to the owning screen, window, dialog, or transient view. Dispose them on every exit path.
- Use `onCleanup` for resources replaced by an effect and `batch` for logically atomic multi-signal updates.
- Model asynchronous request state reactively, including progress/loading, result, and failure. Dispatch worker completion to the GL thread before writing UI-driving signals.
- Prefer fine-grained property binding through Meta bindings. Use a scoped subscription only for coarse structural rebuilds.
- Add tests for initial execution, update propagation, unchanged-value suppression, batching, cleanup, disposal, failure, and cycles when relevant.

### Hot-loop exception

Do not drive render, physics, animation, or other measured per-frame inner loops through signal/effect/computed chains when that creates material dispatch, boxing, or allocation cost. Keep hot data in primitive fields or allocation-free engine structures and move it one way through the loop. Publish a coarser reactive snapshot only when application or UI state must observe it.

This exception is narrow. It does not justify custom observables, manual UI polling, duplicated state, or callback fan-out outside a demonstrated hot path. Require profiling evidence or a clearly per-frame call site.

Treat generic primitive signals as unsuitable for high-frequency writes because JVM generic storage boxes primitives. Prefer primitive fields in hot loops. When reactive primitive propagation is required, use Meta's `BooleanSignal`, `IntSignal`, `LongSignal`, `FloatSignal`, or `DoubleSignal` and their specialized value, peek, update, and subscribe paths. Use inherited `Signal<T>` members only at generic compatibility boundaries because those members box.

## Meta UI and responsive layout

- Reuse Meta widgets and composed controls before creating a component. Do not introduce VisUI or libktx UI layers.
- Use `MetaFlexBox` for rows, columns, forms, toolbars, and wrapping; `MetaGrid` for equal shared tracks; `MetaStack` for overlays; and `MetaTable` only for genuine Scene2D cell or span semantics.
- Make desktop layouts responsive to their parent, not to a one-time global screen measurement. Exercise narrow fallback, 1280, 1920, 2560, and 3840 width classes plus constrained-height cases when applicable.
- Avoid triplicated breakpoint layouts. Keep shared structure and cascade only the properties, visibility, sizing, or direction that differ.
- Use `MetaType`, `MetaSpacing`, `MetaColor`, `MetaButtonTier`, and Meta control-size conventions. Treat shared colors as read-only and copy variants.
- Check labels and other text widgets against the semantic `MetaType` typography scale. Flag magic-number font sizes and locally duplicated or custom-rolled typography scales that bypass or drift from `MetaType`; use the nearest existing semantic token instead.
- Use the bundled Remix icon catalog and semantic Meta icon/button components for routine actions. Do not substitute text glyphs, emoji, or bitmap icons for catalog actions.
- Use TTF-backed Meta text widgets. Clone a skin style before changing its font; never mutate a shared style.
- Use `FontProvider`-generated fonts and Meta pixel-snap/font-refresh helpers. Do not bypass them with ad hoc font caches or unsnapped text drawing that can blur, bleed, or retain a stale scale.
- Use `MetaScrollPane`, `MetaWindow`, and `MetaDialog` according to their ownership contracts instead of layering competing scrolling or window chrome.

For offline geometry verification, initialize `GdxTestEnvironment`, size and validate the root, then use `MetaLayout.problems(root)` or `MetaLayout.assertValid(root)`. Add deterministic tests for breakpoint transitions, visibility, minimum/preferred sizes, nesting, constrained height, and overflow-prone layouts.

## Startup, asset loading, and perceived progress

- For non-trivial startup, use `SplashScreen` with `prepareAssets`, `queueAssets`, and `onLoaded`: keep filesystem discovery, archive indexing, and queue construction off the GL thread, queue through `AssetProvider`, and perform scene2d/GL work only in the GL-thread completion phase.
- Advance `AssetProvider` with frame-budgeted `update` calls while rendering the splash. Do not block animated loading with `finish()` or unqueued `getResource()` calls, and do not let one large upload make the loading screen appear frozen when cooperative budgeting can avoid it.
- Expose real `AssetProvider.progress` when determinate; use an honest indeterminate spinner when work cannot be measured instead of a fake timer. Keep the splash responsive with animated spinners/status text and smooth fade-in, fade-out, and post-load UI transitions.
- Handle preparation and loading failures visibly and preserve cleanup for splash-owned textures, fonts, providers, worker threads, and reactive/UI scopes on every exit path.
- Keep loading progress and transition state reactive where it drives UI, but avoid rebuilding the loading tree or allocating strings/temporary objects every frame.

## Scene2D and reactive lifecycle

- Release global input, visibility leases, capture listeners, controller listeners, focus overrides, subscriptions, and reactive scopes on every exit path.
- Put dialog/window cleanup in their lifecycle teardown (`onHidden` where applicable), not only in confirm/cancel handlers.
- Use `MetaInputProcessor` and its exclusive-processor stack; do not replace `Gdx.input.inputProcessor` or manually emulate global grabs.
- Preserve Scene2D focus authority while text fields or text areas are editing.
- Ensure responsive bindings stop affecting actors after removal, reparenting, clearing, hiding, or disposal.
- Do not allocate in `act`, `draw`, `layout`, `sizeChanged`, or other frame-hot paths. Avoid captured lambdas, iterators, temporary collections, and per-frame strings there.

## Java 25 desktop runtime

- Meta and its consumers target Java 25. Package desktop launchers with `-XX:+UseCompactObjectHeaders` and
  `--enable-native-access=ALL-UNNAMED`; a library cannot enable JVM startup flags after process creation.
- Keep application-specific AOT cache training in the final game's packaging pipeline because the cache depends on
  its complete classpath and representative startup path.

## Audit method and output

1. Inspect the changed code and its call sites before judging the pattern.
2. Search for an existing Meta component, binding, or reactive primitive that replaces custom machinery.
3. Distinguish edge-adapter callbacks from custom state-propagation systems and demonstrated hot loops from ordinary application state.
4. Verify lifecycle ownership, GL-thread writes, responsive geometry, allocation behavior, and tests.
5. Report findings by severity with exact file/line evidence, impact, and the smallest Meta-native correction. Include a regression-test expectation.

If no actionable violations remain, say so and list the checks performed. A review request does not authorize implementation; change code only when the user asks for a fix or implementation.
