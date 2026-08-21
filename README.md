[![CI](https://github.com/Frotty/Meta/actions/workflows/ci.yml/badge.svg)](https://github.com/Frotty/Meta/actions/workflows/ci.yml)

# Meta

Meta is a reusable Kotlin/libGDX runtime layer for JVM games. It combines scene2d UI, reactive state, asset loading,
input routing, dependency injection, persistence, audio, and desktop platform bindings. OxRox is a downstream
consumer; game-specific behavior stays outside this repository.

![Meta UI component showcase](https://github.com/user-attachments/assets/11e32da3-6df5-4d59-8673-a83b65619148)

## Modules

| Module | Purpose |
| --- | --- |
| `runtime` | Platform-neutral engine services, state, assets, input, audio, and UI. |
| `runtime-desktop` | LWJGL3 implementations for desktop games. |
| `editor` | Optional scene and shader editor built on the runtime. |
| `editor-desktop` | Optional desktop editor launcher and UI playground. |

The runtime targets Java 25 and currently builds against Kotlin 2.4.10 and libGDX 1.14.2. The Gradle wrapper is the
authoritative build entry point.

## Use from a game

Pin a tested JitPack commit. Add `runtime` to the core game and `runtime-desktop` only to the desktop launcher:

```groovy
repositories {
    maven { url = 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.Frotty.Meta:runtime:<commit>"
    implementation "com.github.Frotty.Meta:runtime-desktop:<commit>" // desktop launcher only
}
```

Desktop distributions must use a Java 25 runtime. Enable compact object headers in every packaged launcher; JVM
startup options cannot be enabled by a library after the process has started:

```groovy
application {
    applicationDefaultJvmArgs = [
        '-XX:+UseCompactObjectHeaders',
        '--enable-native-access=ALL-UNNAMED', // LWJGL/native loading on Java 25
    ]
}
```

Meta's editor and playground launchers already apply this option. Custom launch scripts should pass the same
arguments to `java`. A trained Java 25 AOT cache is application- and classpath-specific, so create and ship it from
the final game's packaging pipeline rather than Meta's library build.

Meta is a [multi-module JitPack project](https://docs.jitpack.io/building/#multi-module-projects), so the repository
name is part of the group ID. Upgrade and verify Meta before changing a released game's pinned commit; avoid floating
branch versions.

## Runtime conventions

- Build visible text with the TTF-backed `Meta*` widgets, not raw scene2d text controls.
- Compose ordinary layouts with `MetaFlexBox`, `MetaGrid`, or `MetaStack`; use `MetaTable` for real table semantics.
- Use `MetaType`, `MetaSpacing`, `MetaColor`, and `MetaButtonTier` instead of local style constants.
- Use bundled Remix glyphs (`MetaIcon`, `MetaIconButton`, `MetaIconTextButton`) for routine UI icons. Search
  [`assets/ui/icons/remixicon.tsv`](assets/ui/icons/remixicon.tsv) for supported names.
- Use `signal`, `computed`, `effect`, `batch`, and scope-owned bindings from `de.fatox.meta.reactive`. Runtime state and
  scene2d updates belong on the GL thread.
- Use `booleanSignal`, `intSignal`, `longSignal`, `floatSignal`, or `doubleSignal` when primitive reactive state is
  written frequently. Their specialized value/peek/update/subscribe paths avoid generic JVM boxing; each retains a
  `Signal<T>` surface for compatibility boundaries.
- Create `MetaWindow`/`MetaDialog` bindings in `onShown()` using their `reactiveScope`; it is disposed on hide.
- Queue startup assets through `AssetProvider.load`. Use the three-callback `SplashScreen` when discovery or queueing
  does meaningful work off the GL thread.

Create presentation-owned bindings in `onShown()` so cached windows can be hidden and shown without retaining stale
effects:

```kotlin
class CounterWindow : MetaWindow("Counter") {
    private val count = signal(0)
    private val label = MetaLabel()

    init {
        contentTable.add(label)
    }

    override fun onShown() {
        reactiveScope.bindText(label) { "Count: ${count()}" }
    }
}
```

Window chrome, dialog actions, scrolling, docking, pointer cursors, and nested scroll focus have runtime-owned
defaults. Extend those primitives when a reusable behavior is missing instead of rebuilding them in a consumer.

### Responsive layouts

`MetaFlexBox` supports reactive container breakpoints. The base declaration is the narrow-window fallback; larger
values cascade with `from`, and width ranges or height constraints use `MetaResponsiveQuery`. Resizing only the root
is enough: assigned bounds propagate through nested flex boxes and update their `responsiveState` signals.

```kotlin
val content = metaFlexRow {
    addItem(sidebar, basisWidth = 280f)
    addItem(page, grow = 1f)

    responsive {
        direction(MetaFlexDirection.COLUMN)
            .from(MetaBreakpoints.FULL_HD, MetaFlexDirection.ROW)
        gap(MetaSpacing.SM)
            .from(MetaBreakpoints.QHD, MetaSpacing.LG)
        item(sidebar) {
            visible(false).from(MetaBreakpoints.HD, true)
            width(220f).from(MetaBreakpoints.FULL_HD, 280f)
        }
    }
}
```

The sparse desktop scale is `NARROW`, `HD` (1280), `FULL_HD` (1920), `QHD` (2560), and `UHD` (3840), measured in
the container's logical Meta UI units. `NARROW` also covers phone-sized or heavily constrained windows. Use custom
`MetaBreakpoint` values when a component's content calls for a different transition; use
`MetaResponsiveQuery.heightBelow(...)` for short-window variants such as compact navigation at 720p-like heights.

For responsive state outside a flex box, create `MetaResponsiveState`, call `resize` from the owner's resize path,
and consume `breakpoint`, `matches(query)`, or `resolve(responsiveValue)` through normal `computed`/`effect` bindings.

## Develop and verify

Run the same compile and test gates as CI:

```powershell
.\gradlew.bat verifyKotlinIterationSafety :runtime:compileKotlin :runtime-desktop:compileKotlin :editor:compileKotlin :editor-desktop:compileKotlin
.\gradlew.bat :runtime:test
```

For UI work, also run the playground and add geometry or behavior coverage:

```powershell
.\gradlew.bat :editor-desktop:runMetaUiPlayground
```

Production Kotlin must not use iterator-based loops over libGDX collections; the iteration guard runs before
`compileKotlin` and `check`.

## Maintainer reference

[`AGENTS.md`](AGENTS.md) is the concise architecture and contribution contract: module ownership, public API
compatibility, lifecycle cleanup, reactive state, performance rules, startup boundaries, and required verification.
The icon catalog update procedure is documented in [`assets/ui/icons/README.md`](assets/ui/icons/README.md).
