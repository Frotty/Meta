[![CI](https://github.com/Frotty/Meta/actions/workflows/ci.yml/badge.svg)](https://github.com/Frotty/Meta/actions/workflows/ci.yml) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/5d29848d4aa84e46b4e4fb185222c668)](https://www.codacy.com/app/frotty/Meta?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Frotty/Meta&amp;utm_campaign=Badge_Grade)

# Meta
Kotlin/libGDX engine layer with a batteries-included runtime UI toolkit and a scene/shader editor.

## Concept
Meta is used alongside libGDX to provide a more complete out-of-the-box runtime layer for JVM games.
Create a normal libGDX project, add Meta to the Gradle build, and keep your game logic in code while using Meta for
shared runtime services, UI, assets, input, data, and editor workflows.

## Editor
The editor generates project data in readable json format which will be loaded by the engine at runtime.
It combines asset management and scene creation into a simple UI with the capabilities to import and export 2d (soon) and 3d scenes into various formats.

While the editor is recommended for any beginner, advanced users can also decide to use the runtime without it.
Your own source in the JVM language of your choice will always remain the core of your project.

## Meta UI
Meta's UI layer is built around TTF-backed scene2d widgets so games and editor screens share one rendering path:
`MetaLabel`, `MetaTextButton`, `MetaIconTextButton`, `MetaSelectBox`, `MetaTextField`, `MetaCheckBox`, and friends.

The default constructors use the `MetaType.BODY` type scale and generated Meta skin. Compose ordinary content with
`metaRow` and `metaColumn`; they apply the standard spacing rhythm without forcing padding into low-level structural
tables. Use `MetaInputLayout.field` / `area` for label + input + helper/error compositions and `MetaScrollPane` for
scrollable content. Scroll panes automatically follow the mouse and restore an outer pane after leaving a nested one,
so ordinary wheel scrolling needs no focus wiring.

```kotlin
val form = metaColumn {
    add(MetaInputLayout.field("Name", placeholder = "Player name")).row()
    add(metaRow {
        add(MetaTextButton("Cancel"))
        add(MetaIconTextButton("Create", "ri-add-line"))
    }).right().row()
}
```

`MetaIconTextButton` is a compact horizontal action by default. Set `vertical = true` only for explicit icon tiles.
`MetaTable` deliberately remains neutral for structural scene2d layout; opt into `MetaTable(defaultSpacing = true)`
or use the row/column helpers for content layouts.

Meta buttons manage pointer cursors automatically, background clicks release stale text-input focus, and opening a
dialog cancels presses or drags that began behind it. These scene2d input details should not need consumer wiring.
Tooltips can likewise be attached before an actor is shown and survive cached-window detach/re-attach cycles; their
registry owns actors weakly, with `removeTooltip()` available for explicit early cleanup.

UI icons are font-backed through Remix Icon. Prefer:

```kotlin
MetaIcon("ri-information-line")
MetaImageButton("ri-add-line")
MetaIconTextButton("Open", "ri-folder-open-line")
```

The bundled catalog lives at `assets/ui/icons/remixicon.tsv` and is also shipped in runtime resources as
`ui/icons/remixicon.tsv`. Runtime code can query it through `MetaIcons.exists`, `MetaIcons.info`,
`MetaIcons.names`, `MetaIcons.entries`, and `MetaIcons.search`. Do not add one-off PNG toolbar icons; use Remix font
icons unless the asset is actual game/editor art rather than a UI glyph.

## Screenshots
The historical screenshots were removed because they no longer represent the current Meta UI. New screenshots should be
captured from the current editor flow and committed as repo-local documentation assets instead of linking old external
images.


## Runtime
The runtime contains all core components of the meta engine that will aid in the creation of any 2d or 3d game.

### Asynchronous asset loading

Queue startup assets through `AssetProvider.load` from `SplashScreen`. Meta advances the libGDX `AssetManager` using
only the current frame's spare time, keeps the splash responsive, and renders actual queue progress. Put raw/XPK
discovery in `prepareAssets`; it and `queueAssets` then run sequentially on a low-priority worker, while XPK I/O,
hashing, indexing, and extraction yield in bounded chunks. Asset finalization and completion remain on the GL thread:

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

With the three-callback form, `prepareAssets` and `queueAssets` must only perform thread-safe CPU/file/queueing work;
they must not touch OpenGL or scene2d. `onLoaded` runs on the GL thread. The two-callback form keeps `queueAssets` on
the GL thread for compatibility and should only be used for small queues. Calling `getResource` for an asset that was
not preloaded remains supported, but it necessarily blocks until that particular asset is available and should be
kept out of frame-time-sensitive paths. Individual OpenGL uploads are atomic in libGDX; avoid exceptionally large
standalone textures if a single upload still exceeds the frame budget. Keep `onLoaded` and the first screen's
constructor lightweight as well; expensive screen setup after the queue completes cannot be animated by the splash.

### Dependency Injection
Meta comes with a lightweight DI framework, focusing mainly on property injection.
Dependencies are provided via modules like the renderer for the editor in this example:

```kotlin
MetaInject.global {
    singleton<Renderer> { EditorSceneRenderer() }
}
```

You can now inject this Singleton into any class.

```kotlin
class GameScreen : ScreenAdapter {
    private val renderer: Renderer by lazyInject() // shortcut for lazy { inject() }
    // OR
    private val renderer: Renderer = inject()
}
```
Dependency Injection allows for fast and managed object creation with easy customization and later plugin possibilities.
Default injections are named so that when the user provides their own provider, he can override the existing behaviour of that class.
Inside a game the renderer could be replaced:
```kotlin
MetaInject.global {
    singleton<Renderer> { GameSceneRenderer() }
}
```
