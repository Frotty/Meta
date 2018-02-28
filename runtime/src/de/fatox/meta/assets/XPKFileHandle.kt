package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import java.io.ByteArrayInputStream
import java.io.InputStream

class XPKFileHandle(var input: ByteArray, val fileType: XPKTypes, val name: String) : FileHandle() {

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

    override fun path(): String {
        return name
    }

    override fun pathWithoutExtension(): String {
        return name.substring(0, name.lastIndexOf(".") - 1)
    }

    override fun length(): Long {
        return input.size.toLong()
    }
}