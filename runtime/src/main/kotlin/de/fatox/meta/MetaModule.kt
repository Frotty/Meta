package de.fatox.meta

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShaderProgram.*
import com.badlogic.gdx.utils.Json
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.entity.EntityManager
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.entity.MetaEntityManager
import de.fatox.meta.graphics.font.FontInfo
import de.fatox.meta.graphics.font.MetaFontProvider
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.input.MetaInput
import de.fatox.meta.sound.MetaSoundPlayer
import de.fatox.meta.task.MetaTaskManager
import de.fatox.meta.ui.MetaUIRenderer
import de.fatox.meta.ui.MetaUiManager
import de.fatox.meta.ui.UiControlHelper
import org.intellij.lang.annotations.Language

object MetaModule {
	init {
		MetaInject.global {
			singleton<FontProvider>("default") { MetaFontProvider() }
			singleton { MetaSoundPlayer() }
			singleton<UIRenderer> { MetaUIRenderer() }
			singleton<UIManager> { MetaUiManager() }
			singleton { ModelBuilder() }
			singleton<MetaInputProcessor> { MetaInput() }
			singleton<EntityManager<Meta3DEntity>>("default") { MetaEntityManager() }
			singleton("default") { MetaTaskManager() }
			singleton("", "default")
			singleton(Json(), "default")
			singleton("default") { UiControlHelper() }
			singleton("default") { FontInfo("Montserrat.ttf", "Montserrat-Bold.ttf","RobotoMono.ttf") }
			singleton("default") {
				SpriteBatch(1000, inject("spritebatch-shader")).apply { enableBlending() }
			}
			singleton {
				PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
					position.set(0f, 50f, 50f)
					near = 0.1f
					far = 200f
				}
			}
			singleton("spritebatch-shader") {
				@Suppress("SpellCheckingInspection")
				@Language("GLSL") val vertexShader =
					"""
in vec4 $POSITION_ATTRIBUTE;
in vec4 $COLOR_ATTRIBUTE;
in vec2 ${TEXCOORD_ATTRIBUTE}0;
uniform mat4 u_projTrans;
out vec4 v_color;
out vec2 v_texCoords;

void main()
{
   v_color = $COLOR_ATTRIBUTE;
   v_color.a = v_color.a * (255.0/254.0);
   v_texCoords = ${TEXCOORD_ATTRIBUTE}0;
   gl_Position =  u_projTrans * $POSITION_ATTRIBUTE;
}
""".trimMargin()

				@Suppress("SpellCheckingInspection")
				@Language("GLSL")
				val fragmentShader =
					"""
#ifdef GL_ES
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
}
""".trimMargin()
				val shader = ShaderProgram(vertexShader, fragmentShader)
				require(shader.isCompiled) { "Error compiling shader: ${shader.log}" }
				shader
			}
		}
	}
}
