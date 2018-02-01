package de.fatox.meta.api.graphics

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.model.GLShaderData
import de.fatox.meta.api.model.RenderTargetData
import java.io.BufferedReader
import java.io.IOException
import java.util.regex.Pattern

/**
 * Created by Frotty on 02.07.2016.
 */
class GLShaderHandle(val shaderHandle: FileHandle, vertexHandle: FileHandle?, fragmentHandle: FileHandle?, var data: GLShaderData) {
    var fragmentHandle = fragmentHandle
        set(value) {
            data.fragmentFilePath = fragmentHandle?.path() ?: ""
        }
    var vertexHandle = vertexHandle
        set(value) {
            data.vertexFilePath = vertexHandle?.path() ?: ""
        }

    var targets = Array<RenderTargetData>()

    init {
        fetchRendertargets()
    }

    private fun fetchRendertargets() {
        targets.clear()
        try {
            BufferedReader(fragmentHandle!!.reader()).use { br ->
                var line: String? = br.readLine()
                while (line != null) {
                    if (line.startsWith("layout")) {
                        val matcher = outPattern.matcher(line)
                        if (matcher.matches()) {
                            val type = matcher.group(4)
                            val name = matcher.group(6)
                            targets.add(RenderTargetData(type, name))
                        }
                    }
                    line = br.readLine()
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

    override fun toString(): String {
        return data.name
    }

    companion object {

        private val outPattern = Pattern.compile("(.*)(out)(\\s)+(vec[2-4])(\\s)+(\\w+)(;)")
    }
}
