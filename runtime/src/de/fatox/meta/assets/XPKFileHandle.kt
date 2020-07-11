package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.GdxRuntimeException
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.ByteArrayInputStream
import java.io.InputStream

class XPKFileHandle(val siblings: Array<XPKFileHandle>, var length: Int, val sevenZFile: SevenZFile, val entry: SevenZArchiveEntry, val name: String) : FileHandle() {
    val path: String = name.substring(0, name.lastIndexOf("\\").takeIf { it >= 0 } ?: name.length)
    val fileName: String = name.substring((name.lastIndexOf("\\").takeIf { it >= 0 } ?: -1) + 1)
    var array: ByteArray? = null

    override fun exists(): Boolean {
        return true
    }

    override fun parent(): FileHandle {
        var name = ""
        if (path != this.name) {
            name = path
        }
        return XPKFileHandle(siblings, 0, sevenZFile, entry, name)
    }

    override fun sibling(name: String?): FileHandle? {
        return siblings.find {
            it.path == path && it.fileName == name
        }
    }

    override fun read(): InputStream {
        try {
            return ByteArrayInputStream(readBytes())
        } catch (ex: Exception) {
            if (file() != null && file().isDirectory)
                throw GdxRuntimeException("Cannot open a stream to a directory: $file ($type)", ex)
            throw GdxRuntimeException("Error reading file: $file ($type)", ex)
        }

    }

    override fun name(): String {
        return name
    }

    override fun extension(): String {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
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
        return length.toLong()
    }

    override fun child(name: String): FileHandle? {
        var search = name
        if (path != name && !path.isBlank()) {
            search = path + "\\" + name
        }
        return siblings.find {
            it.name.replace("/", "\\") == search
        } ?: XPKFileHandle(siblings, 0, sevenZFile, entry, search)
    }

    override fun toString(): String {
        return name.replace("\\", "/")
    }

    override fun readBytes(): ByteArray? {
        if (array == null) {
            array = XPKLoader.loadEntry(sevenZFile, entry)
        }
        return array
    }
}