package de.fatox.meta.api.graphics

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.model.GLShaderData
import de.fatox.meta.api.model.RenderTargetData
import java.io.BufferedReader
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by Frotty on 02.07.2016.
 */
class GLShaderHandle(
	val shaderHandle: FileHandle,
	vertexHandle: FileHandle,
	fragmentHandle: FileHandle,
	var data: GLShaderData,
) {
	var fragmentHandle = fragmentHandle
		set(value) {
			field = value.also { data.fragmentFilePath = it.path() }
		}
	var vertexHandle = vertexHandle
		set(value) {
			field = value.also { data.vertexFilePath = it.path() }
		}

	val targets: Array<RenderTargetData> = Array<RenderTargetData>()

	override fun toString(): String = data.name

	init {
		targets.clear()
		try {
			BufferedReader(this.fragmentHandle.reader()).use { br ->
				var line: String
				while (br.readLine().also { line = it } != null) {
					if (line.startsWith("layout")) {
						val matcher: Matcher = outPattern.matcher(line)
						if (matcher.matches()) {
							val type = matcher.group(4)
							val name = matcher.group(6)
							targets.add(RenderTargetData(type, name))
						}
					}
				}
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
		if (targets.size == 0) {
			// add default gl out
			targets.add(RenderTargetData("vec4", "gl_FragColor"))
		}
	}
}

private val outPattern: Pattern = Pattern.compile("(.*)(out)(\\s)+(vec[2-4])(\\s)+(\\w+)(;)")