package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.GdxRuntimeException
import java.io.ByteArrayInputStream
import java.io.InputStream

class XPKFileHandle(val siblings: Array<XPKFileHandle>, var input: ByteArray, val fileType: XPKTypes, val name: String) : FileHandle() {
    val path: String = name.substring(0, name.lastIndexOf("\\").takeIf { it >= 0 } ?: name.length)
    val fileName: String = name.substring((name.lastIndexOf("\\").takeIf { it >= 0 } ?: -1) + 1)

    override fun exists(): Boolean {
        return true
    }

    override fun parent(): FileHandle {
        return this
    }

    override fun sibling(name: String?): FileHandle? {
        return siblings.find {
            it.path == path && it.fileName == name
        }
    }

    override fun read(): InputStream {
        try {
            return ByteArrayInputStream(input)
        } catch (ex: Exception) {
            if (file().isDirectory)
                throw GdxRuntimeException("Cannot open a stream to a directory: $file ($type)", ex)
            throw GdxRuntimeException("Error reading file: $file ($type)", ex)
        }

    }

    override fun name(): String {
        return name
    }

    override fun nameWithoutExtension(): String {
        return fileName.substring(0, fileName.lastIndexOf("."))
    }

    override fun path(): String {
        return name
    }

    override fun pathWithoutExtension(): String {
        return name.substring(0, name.lastIndexOf("."))
    }

    override fun length(): Long {
        return input.size.toLong()
    }

    override fun child(name: String?): FileHandle? {
        val search = path + "\\" + name
        return siblings.find {
            it.name.replace("/", "\\") == search
        } ?: XPKFileHandle(siblings, byteArrayOf(), XPKTypes.INVALID, search)
    }

    override fun toString(): String {
        return name.replace("\\", "/")
    }
}