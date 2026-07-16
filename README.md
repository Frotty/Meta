[![CI](https://github.com/Frotty/Meta/actions/workflows/ci.yml/badge.svg)](https://github.com/Frotty/Meta/actions/workflows/ci.yml)

# Meta

Meta is a Kotlin/libGDX runtime layer for JVM games. It provides a consistent scene2d UI toolkit, reactive state,
asset loading, input routing, dependency injection, persistence, audio/video state, and desktop platform bindings.
OxRox is a downstream consumer.

The editor modules are optional tools built on the same runtime. Games can depend on the runtime without shipping the
editor.

<img width="1276" height="896" alt="Meta UI component showcase" src="https://github.com/user-attachments/assets/11e32da3-6df5-4d59-8673-a83b65619148" />

## Modules

- `runtime`: platform-neutral engine services and UI components.
- `runtime-desktop`: LWJGL3 desktop integrations.
- `editor`: optional scene/shader/editor functionality.
- `editor-desktop`: optional editor launcher.

## Consumer setup

Meta is consumed from JitPack using a pinned commit. A desktop game normally uses `runtime` in its core module and
`runtime-desktop` in its launcher module:

```groovy
repositories {
    maven { url = 'https://jitpack.io' }
}

dependencies {
    implementation "com.github.frotty.meta:runtime:<commit>"
    implementation "com.github.frotty.meta:runtime-desktop:<commit>" // desktop launcher only
}
```

Upgrade Meta first, verify it, then change the consumer's pinned commit. Do not use a floating branch or snapshot for
released games.

## Runtime UI

Meta's scene2d UI uses generated skin drawables and TTF-backed widgets. Prefer the `Meta*` components over raw
scene2d text controls:

- `MetaLabel`, `MetaTextButton`, `MetaIconTextButton`, `MetaIconButton`
- `MetaTextField`, `MetaTextArea`, `MetaSelectBox`, `MetaCheckBox`, `MetaSpinner`
- `MetaInputLayout`, `MetaScrollPane`, `MetaBottomBar`, `MetaWindow`, `MetaDialog`

Use `MetaType`, `MetaSpacing`, `MetaColor`, and `MetaButtonTier` instead of consumer-specific styles and magic
numbers. `metaRow`, `metaColumn`, and `MetaTable(defaultSpacing = true)` provide the normal content spacing rhythm.

```kotlin
val form = metaColumn {
    add(MetaInputLayout.field("Name", placeholder = "Player name")).row()
    add(metaRow {
        add(MetaTextButton("Cancel", tier = MetaButtonTier.TERTIARY))
        add(MetaIconTextButton("Create", "ri-add-line", tier = MetaButtonTier.PRIMARY))
    }).right().row()
}
```

Meta buttons manage pointer cursors. `MetaScrollPane` manages nested scroll focus. Window chrome, title separators,
dialog action spacing, dock reflow, and constrained content scrolling are runtime-owned behavior; consumers should not
reimplement them.

### Icons

Ordinary UI glyphs use the bundled Remix Icon font:

```kotlin
MetaIcon("ri-information-line")
MetaImageButton("ri-add-line")
MetaIconTextButton("Open", "ri-folder-open-line")
```

Search [`assets/ui/icons/remixicon.tsv`](assets/ui/icons/remixicon.tsv) or use `MetaIcons.search`. Bitmap assets remain
appropriate for game art, logos, previews, screenshots, and atlases—not routine toolbar glyphs.

## Reactive state and lifecycle

`de.fatox.meta.reactive` is the runtime's state system:

```kotlin
val count = signal(0)
val summary = computed { "Count: ${count()}" }

reactiveScope.bindText(label) { summary() }
```

Use `signal`, `computed`, `effect`, `batch`, and `subscribe` instead of custom observer lists. Widget model signals such
as `textValue`, `checkedValue`, `disabledValue`, and `selectedValue` are writable and update their widgets in both
directions.

`MetaWindow` and `MetaDialog` provide a presentation-scoped `reactiveScope`; create bindings in `onShown()`. Screens
can extend `ReactiveScreenAdapter` for the same show/hide lifecycle. Reactive state is GL-thread-only: dispatch worker
results through `Gdx.app.postRunnable` before writing signals that drive UI.

Undo/redo UI can bind directly to `MetaTaskManager.canUndo` and `canRedo`.

## Asynchronous startup loading

Queue startup assets through `AssetProvider.load` and let `SplashScreen` advance libGDX's `AssetManager` in
frame-adaptive slices. The splash displays real progress and remains animated while work is chunked correctly.

```kotlin
SplashScreen(
    prepareAssets = {
        assetProvider.loadRawAssetsFromFolder(Gdx.files.internal("."))
        assetProvider.loadPackedAssetsFromFolder(Gdx.files.internal("data"))
    },
    queueAssets = {
        assetProvider.load<Texture>("textures/player.png")
        assetProvider.load<Model>("models/world.g3db")
    },
    onLoaded = {
        Meta.changeScreen(GameScreen())
    },
)
```

The three-callback form runs `prepareAssets` and `queueAssets` sequentially on a low-priority worker. They may perform
CPU work, file discovery, XPK indexing, and thread-safe queue construction, but must not touch OpenGL or scene2d.
`onLoaded` runs on the GL thread.

The two-callback compatibility form runs `queueAssets` on the GL thread and is only suitable for small queues.
`getResource` blocks when an asset was not preloaded, so keep it out of frame-sensitive paths. Large individual GL
uploads are atomic in libGDX and cannot be subdivided by the splash budget.

XPK remains an implementation detail of the asset layer. Use `XPKLoader.listEntryNames` for inspection or
`XPKLoader.getList` when lazy `FileHandle` reads are required; do not expose Apache Commons Compress types from public
Meta APIs.

## Dependency injection

Register shared services with `MetaInject` and inject them eagerly or lazily:

```kotlin
MetaInject.global {
    singleton<Renderer> { GameSceneRenderer() }
}

class GameScreen : ReactiveScreenAdapter() {
    private val renderer: Renderer by lazyInject()
}
```

Named default providers can be replaced by a consumer during startup.

## Development

The Gradle wrapper is authoritative:

```powershell
.\gradlew.bat :runtime:compileKotlin
.\gradlew.bat :runtime:test
.\gradlew.bat :runtime-desktop:compileKotlin :editor:compileKotlin :editor-desktop:compileKotlin
```

Run the UI playground with:

```powershell
.\gradlew.bat :editor-desktop:runMetaUiPlayground
```

See [`AGENTS.md`](AGENTS.md) for architecture, performance, lifecycle, compatibility, and contribution contracts.
