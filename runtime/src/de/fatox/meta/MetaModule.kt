package de.fatox.meta

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.Json
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.entity.EntityManager
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.entity.MetaEntityManager
import de.fatox.meta.graphics.font.MetaFontProvider
import de.fatox.meta.injection.Named
import de.fatox.meta.injection.Provides
import de.fatox.meta.injection.Singleton
import de.fatox.meta.input.MetaInput
import de.fatox.meta.sound.MetaSoundPlayer
import de.fatox.meta.task.MetaTaskManager
import de.fatox.meta.ui.MetaUIRenderer
import de.fatox.meta.ui.MetaUiManager
import de.fatox.meta.ui.UiControlHelper

@Singleton
class MetaModule {

	@Provides
    @Named("default")
    fun string(): String {
        return ""
    }

    @Provides
    @Singleton
    fun metaSoundPlayer(): MetaSoundPlayer {
        return MetaSoundPlayer()
    }

    @Provides
    @Singleton
    fun uiRenderer(): UIRenderer {
        return MetaUIRenderer()
    }

    @Provides
    @Singleton
    fun uiManager(): UIManager {
        return MetaUiManager()
    }

    @Provides
    @Singleton
    fun metaInput(): MetaInput {
        return MetaInput()
    }

    //    @Provides
    //    @Singleton
    //    @Named("default")
    //    public Renderer renderer(BufferRenderer renderer) {
    //        return renderer;
    //    }

    @Provides
    @Singleton
    fun perspectiveCamera(): PerspectiveCamera {
        val cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.position.set(0f, 50f, 50f)
        cam.near = 0.1f
        cam.far = 200f
        return cam
    }

    @Provides
    @Singleton
    fun modelBuilder(): ModelBuilder {
        return ModelBuilder()
    }

    @Provides
    @Singleton
    @Named("default")
    fun entityManager(): EntityManager<Meta3DEntity> {
        return MetaEntityManager()
    }


    @Provides
    @Singleton
    @Named("default")
    fun spriteBatch(): SpriteBatch {
        val spriteBatch = SpriteBatch()
        spriteBatch.enableBlending()
        return spriteBatch
    }

    @Provides
    @Singleton
    @Named("default")
    fun assetProvider(metaAssetProvider: MetaAssetProvider): AssetProvider {
        return metaAssetProvider
    }

    @Provides
    @Singleton
    @Named("default")
    fun fontProvider(metaFontProvider: MetaFontProvider): FontProvider {
        return metaFontProvider
    }


    @Provides
    @Singleton
    @Named("default")
    fun taskManager(): MetaTaskManager {
        return MetaTaskManager()
    }

    @Provides
    @Singleton
    @Named("default")
    fun json(): Json {
        return Json()
    }

    @Provides
    @Singleton
    @Named("default")
    fun uiControlHelp(): UiControlHelper {
        return UiControlHelper()
    }



}
