package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.file.FileChooser
import de.fatox.meta.Meta
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.Inject
import de.fatox.meta.injection.Named
import de.fatox.meta.input.MetaInput
import org.slf4j.LoggerFactory


class MetaUIRenderer : UIRenderer {
	private val log = LoggerFactory.getLogger(MetaUiManager::class.java)

    @Inject
    private lateinit var metaInput: MetaInput
    @Inject
    private lateinit var assetProvider: AssetProvider
    @Inject
    @Named("visuiSkin")
    private lateinit var visuiSkin: String
	@Inject
	private lateinit var spriteBatch: SpriteBatch
    private var stage: Stage;

    init {
        Meta.inject(this)
		stage = Stage(ScreenViewport(), spriteBatch)
        log.debug("Injected MetaUi")
    }

    override fun load() {
        if (visuiSkin != "") {
            VisUI.load(assetProvider[visuiSkin, FileHandle::class.java])
        } else {
            VisUI.load()
        }
        FileChooser.setDefaultPrefsName("de.fatox.meta")
        log.debug("Loaded VisUi")
        VisUI.setDefaultTitleAlign(Align.center)
        stage.root.addCaptureListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (!(event!!.target is TextField || event.target is ScrollPane)) stage.scrollFocus = null
                return false
            }
        })
        metaInput.addGlobalAdapter(stage)
    }





    override fun addActor(actor: Actor) {
        try {
            stage.addActor(actor)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun update() {
        stage.act(Gdx.graphics.deltaTime)
    }

    override fun draw() {
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun getCamera(): Camera {
        return stage.camera
    }

}
