package de.fatox.meta.graphics.renderer

import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable

/**
 * Encapsulates a fullscreen quad, geometry is aligned to the screen corners.
 *
 * @author bmanuel
 */
class FullscreenQuad(height: Float): Disposable {
	private val quad: Mesh = createFullscreenQuad(height)

	override fun dispose() {
		quad.dispose()
	}

	/**
	 * Renders the quad with the specified shader program.
	 */
	fun render(program: ShaderProgram?) {
		quad.render(program, GL30.GL_TRIANGLE_FAN, 0, 4)
	}

	private fun createFullscreenQuad(height: Float): Mesh {
		val nheight = 1f - (1f - height) * 0.5f
		val verts = floatArrayOf(-1f, -1f, 0f, 0f, 0f, 1f, -1f, 0f, 1f, 0f, 1f, height, 0f, 1f, nheight, -1f, height, 0f, 0f, nheight)
		val mesh = Mesh(true, 4, 0,
			VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
			VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))
		mesh.setVertices(verts)
		return mesh
	}
}