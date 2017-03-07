[![Build Status](https://travis-ci.org/Frotty/Meta.svg?branch=master)](https://travis-ci.org/Frotty/Meta) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/5d29848d4aa84e46b4e4fb185222c668)](https://www.codacy.com/app/frotty/Meta?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Frotty/Meta&amp;utm_campaign=Badge_Grade)
# Meta
Conceptual 3D Rendering engine build ontop of libGDX
## Runtime
The runtime contains all core components of the meta engine
### Metastasis
One of the core pieces of Meta is a lightweight Dependency Injection framework inspired by Feather DI.

Metastasis focuses more on Field Injection _(I prefer field injection because it removes the need for a big constructor, 
being able to see which fields are injected and I simply call the injection from the class itself insteaf o using factories)_ 
and offers some additional features.

Simple Interface injection example:
```java
//Module
@Provides
@Singleton
@Named("default")
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
The default renderer could for example be overwritten by a user module.
Some might argue against the inject call in the constructor, but I prefer this over static factory methods or spring magic because it is easier to debug and outputs better error messages (which I also improved btw)
and you can decide when to (lazy) inject as well.

Automatic lazy injection might come in the future if it is neccessary for larger amounts of entities.


## Editor
The editor can be used manage your workspace inclusing assets, shaders, plugins, etc.
Use the world editor to create (2d and) 3d scenes and export them to json.
