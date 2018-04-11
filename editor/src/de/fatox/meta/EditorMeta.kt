package de.fatox.meta

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.Inject
import de.fatox.meta.modules.MetaEditorModule
import de.fatox.meta.modules.MetaUIModule
import de.fatox.meta.screens.MetaEditorScreen
import de.fatox.meta.screens.SplashScreen

class EditorMeta(posM: PosModifier) : Meta(posM) {

    @Inject
    private lateinit var metaData: MetaData
    @Inject
    private lateinit var uiManager: UIManager
    @Inject
    private lateinit var assetProvider: AssetProvider

    init {
        Meta.addModule(MetaEditorModule())
        Meta.addModule(MetaUIModule())
    }

    override fun create() {
        Meta.inject(this)
        uiManager.posModifier = this.modifier
        val array = Array<FileHandle>()
        array.add(Gdx.files.internal("data/"))
        Meta.changeScreen(SplashScreen({
            array.forEach { it -> assetProvider.loadAssetsFromFolder(it) }
            val audioVideoData = metaData.get("audioVideoData", MetaAudioVideoData::class.java)
            changeScreen(MetaEditorScreen())
            uiManager.moveWindow(audioVideoData.x, audioVideoData.y)
            audioVideoData.apply()
        }))
    }

}
