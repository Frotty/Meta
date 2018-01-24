[![Build Status](https://travis-ci.org/Frotty/Meta.svg?branch=master)](https://travis-ci.org/Frotty/Meta) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/5d29848d4aa84e46b4e4fb185222c668)](https://www.codacy.com/app/frotty/Meta?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Frotty/Meta&amp;utm_campaign=Badge_Grade)
# Meta
Conceptual 2D and 3D rendering engine + scene editor built on top of libGDX.

## Concept
Meta is supposed to be used along side libgdx and provide a more complete out of the box experience.
The philosophy stays the same however, so you should setup a libgdx project, then add meta to the gradle.build to get access to the runtime.
This means you will have a pure code project, which you can augment with the editor.

## Editor
The editor generates project data in readable json format which will be loaded by the engine at runtime.
It combines asset management and scene creation into a simple UI with the capabilities to import and export 2d (soon) and 3d scenes into various formats.

While the editor is recommended for any beginner, advanced users can also decide to use the runtime without it.
Your own source in the JVM language of your choice will always remain the core of your project.

(Early screenshot)
![WIP Screen](https://i.imgur.com/cwOhhYn.png)

## Runtime
The runtime contains all core components of the meta engine that will aid in the creation of any 2d or 3d game.

### Dependency Injection
Meta comes with a lightweight DI framework, focusing mainly on property injection.
Dependencies are provided via modules like the renderer for the editor in this example

```kotlin
@Provides
@Singleton
fun renderer() : Renderer {
    return EditorSceneRenderer()
}
```

You can now inject this Singleton into any class.

```kotlin
class GameScreen : ScreenAdapter {
    @Inject
    private lateinit var renderer: Renderer
    
    init {
        Meta.inject(this)
    }
    
}
```
Dependency Injection allows for fast and managed object creation with easy customization and later plugin possibilities.
Default injections are named so that when the user provides it's own provider, he can override the existing behaviour of that class.
Inside a game the renderer could be replaced:
```kotlin
@Provides
@Singleton
fun renderer() : Renderer {
    return GameSceneRenderer()
}
```




