package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import de.fatox.meta.Meta
import de.fatox.meta.api.graphics.Renderer
import de.fatox.meta.injection.Inject
import de.fatox.meta.shader.EditorSceneRenderer
import de.fatox.meta.shader.MetaSceneHandle
import de.fatox.meta.shader.MetaShaderComposer

/**
 * Created by Frotty on 16.06.2016.
 */
class SceneWidget(sceneHandle: MetaSceneHandle) : Widget() {
    @Inject
    private lateinit var renderer: Renderer
    @Inject
    private lateinit var composer: MetaShaderComposer

    init {
        Meta.inject(this)
        (renderer as EditorSceneRenderer).sceneHandle = sceneHandle
        composer.addListener { layout() }
    }

    override fun layout() {
        renderer.rebuild(width.toInt(), height.toInt())
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.end()
        renderer.render(x, y)
        batch.begin()
    }

}
