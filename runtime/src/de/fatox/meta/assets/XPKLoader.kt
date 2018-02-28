package de.fatox.meta.assets

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import org.apache.commons.compress.archivers.sevenz.SevenZFile

enum class XPKTypes(val letter: String, val cb: Class<out Disposable>?) {

    INVALID("", null), SOUND("s", Sound::class.java), MUSIC("m", Music::class.java), TEXTURE("t", Texture::class.java), SHADER("x", Texture::class.java)
}

object XPKLoader {
    const val EXTENSION = "xpk"


    fun detectFileType(letter: String): XPKTypes {
        XPKTypes.values().forEach {
            if (it.letter == letter) {
                return it
            }
        }
        return XPKTypes.INVALID
    }

    fun getList(fileHandle: FileHandle): Array<XPKFileHandle> {
        val array = Array<XPKFileHandle>()
        val sevenZFile = SevenZFile(fileHandle.file())
        val detectFileType = detectFileType(fileHandle.name().substring(fileHandle.name().length - 1))
        sevenZFile.entries.forEach({
            val content = ByteArray(it.size.toInt())
            var offset = 512
            var pos = 0
            while (pos < content.size) {
                sevenZFile.read(content, 0, offset);
                pos += offset
            }
            val xpkFileHandle = XPKFileHandle(content, detectFileType)
            array.add(xpkFileHandle)
        })
        return array
    }

}