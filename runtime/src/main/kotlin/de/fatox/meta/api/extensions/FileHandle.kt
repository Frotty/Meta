package de.fatox.meta.api.extensions

import com.badlogic.gdx.files.FileHandle
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Writes [bytes] to this file via a sibling temp file + atomic rename, so a crash or power loss mid-write can't
 * leave a truncated/corrupt file at the final path (plain [FileHandle.writeBytes] writes straight to the destination).
 */
fun FileHandle.writeBytesAtomic(bytes: ByteArray) {
	val tmp = sibling(name() + ".tmp")
	tmp.writeBytes(bytes, false)
	Files.move(
		tmp.file().toPath(),
		file().toPath(),
		StandardCopyOption.ATOMIC_MOVE,
		StandardCopyOption.REPLACE_EXISTING,
	)
}
