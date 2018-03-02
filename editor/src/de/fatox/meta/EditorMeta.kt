package de.fatox.meta

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.modules.MetaEditorModule
import de.fatox.meta.modules.MetaUIModule
import de.fatox.meta.screens.SplashScreen
import de.fatox.meta.sound.MetaMusicPlayer
import de.fatox.meta.sound.MetaSoundPlayer

class EditorMeta : Meta() {

    private val metaMusicPlayer: MetaMusicPlayer? = null

    private val metaSoundPlayer: MetaSoundPlayer? = null

    init {
        Meta.addModule(MetaEditorModule())
        Meta.addModule(MetaUIModule())
    }

    override fun create() {
        Meta.inject(this)
        val array = Array<FileHandle>()
        array.add(Gdx.files.internal("data/"))
        Meta.changeScreen(SplashScreen(array))
    }

}
