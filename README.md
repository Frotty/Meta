[![Build Status](https://travis-ci.org/Frotty/Meta.svg?branch=master)](https://travis-ci.org/Frotty/Meta) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/5d29848d4aa84e46b4e4fb185222c668)](https://www.codacy.com/app/frotty/Meta?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Frotty/Meta&amp;utm_campaign=Badge_Grade)
# Meta
Conceptual 3D Rendering engine build ontop of libGDX

## Editor
The editor can be used manage your workspace inclusing assets, shaders, plugins, etc.
Use the world editor to create (2d and [soon]) 3d scenes and export them to json.
(Very early screenshot)
![WIP Screen](https://i.imgur.com/BP5ACCC.png)

## Runtime
The runtime contains core components of the meta engine that can be used for any 2d or 3d game.
### Metastasis
Metastasis is a lightweight Dependency Injection framework inspired by Feather DI.

Metastasis focuses more on Field Injection and provides some additional features.

Simple Interface injection example:
```java
//Module
@Provides
public Renderer renderer(BufferRenderer renderer) {
    return renderer;
}

//Usage
public class GameScreen extends ScreenAdapter {
    @Inject
    private Renderer renderer;
    
    public GameScreen() {
        Meta.inject(this);
    }
    ...
}
```
The default renderer could be overwritten by a user module.
Some might argue against the inject call in the constructor, but I prefer this over static factory methods or spring magic because it is easier to debug and outputs better error messages (which are also improved in metastasis)
and you can decide when to (lazily) inject as well.

Injection works with and is encouraged to use with inheritance. The inject call in the super constructor will inject all fields of the subclass as well.

