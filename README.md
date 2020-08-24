![](https://i.imgur.com/8M6CSMh.png)

[![Build Status](https://travis-ci.org/Frotty/Meta.svg?branch=master)](https://travis-ci.org/Frotty/Meta) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/5d29848d4aa84e46b4e4fb185222c668)](https://www.codacy.com/app/frotty/Meta?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Frotty/Meta&amp;utm_campaign=Badge_Grade)

# Meta
Conceptual 2D and 3D engine with scene and shader editor, build on top of libGDX.

## Concept
Meta is supposed to be used alongside libgdx and provide a more complete out of the box experience.
The philosophy stays the same however, so you should setup a libgdx project, then add meta to the gradle.build to get access to the runtime.
This means you will have a pure code project, which you can augment with the editor.

## Editor
The editor generates project data in readable json format which will be loaded by the engine at runtime.
It combines asset management and scene creation into a simple UI with the capabilities to import and export 2d (soon) and 3d scenes into various formats.

While the editor is recommended for any beginner, advanced users can also decide to use the runtime without it.
Your own source in the JVM language of your choice will always remain the core of your project.

(Early screenshots)
![Final Screen](https://i.imgur.com/7n8eZ1r.png)
![Final Screen](https://i.imgur.com/SyWxgYH.png)


## Runtime
The runtime contains all core components of the meta engine that will aid in the creation of any 2d or 3d game.

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
