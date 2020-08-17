package de.fatox.meta

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Json
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.entity.EntityManager
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.entity.MetaEntityManager
import de.fatox.meta.graphics.font.FontInfo
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
	@Singleton
	@Named("default")
	fun assetProvider(metaAssetProvider: MetaAssetProvider): AssetProvider {
		return metaAssetProvider
	}

	@Provides
    @Named("default")
    fun string(): String {
        return ""
    }

	@Provides
	@Named("spritebatch-shader")
	@Singleton
	fun shaderProgram(): ShaderProgram {
		if (Gdx.app.getType().equals(Application.ApplicationType.Desktop)) {
			ShaderProgram.prependVertexCode =  "#version 140\n"
			ShaderProgram.prependFragmentCode =  "#version 140\n"
		} else {
			ShaderProgram.prependVertexCode = "#version 300 es\n"
			ShaderProgram.prependFragmentCode = "#version 300 es\n"
		}
			val vertexShader = """in vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
in vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
in vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
uniform mat4 u_projTrans;
out vec4 v_color;
out vec2 v_texCoords;

void main()
{
   v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
   v_color.a = v_color.a * (255.0/254.0);
   v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
   gl_Position =  u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE};
}
"""
			val fragmentShader = """#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif
in LOWP vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragData;
void main()
{
  fragData = v_color * texture(u_texture, v_texCoords);
}"""
			val shader = ShaderProgram(vertexShader, fragmentShader)
			require(shader.isCompiled) { "Error compiling shader: " + shader.log }
			return shader
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
		val spriteBatch = SpriteBatch(1000, shaderProgram())
        spriteBatch.enableBlending()
        return spriteBatch
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

	@Provides
	@Singleton
	@Named("default")
	fun fontInfo(): FontInfo {
		return FontInfo("Montserrat.ttf", "RobotoMono.ttf")
	}

}
