package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import de.fatox.meta.api.graphics.Renderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.Disposable
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.shader.EditorSceneRenderer
import de.fatox.meta.shader.MetaSceneHandle
import de.fatox.meta.shader.MetaShaderComposer

/**
 * Created by Frotty on 16.06.2016.
 */
class SceneWidget(val sceneHandle: MetaSceneHandle) : Widget() {
	private val renderer: Renderer by lazyInject()
	private val composer: MetaShaderComposer by lazyInject()
	private val changesSubscription: Disposable

	init {
		(renderer as EditorSceneRenderer).sceneHandle = sceneHandle
		changesSubscription = composer.changes.subscribe { layout() }
	}

	override fun layout() {
		renderer.rebuild(width.toInt(), height.toInt())
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		batch.end()
		renderer.render(x, y)
		batch.begin()
	}

	/** Releases the subscription to the composer's global change signal; called when the owning tab closes. */
	fun dispose() {
		changesSubscription.dispose()
	}
}
