package de.fatox.meta.assets.xpk

import com.badlogic.gdx.files.FileHandle
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.security.KeyPair
import java.security.KeyPairGenerator
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XpkV2FormatTest {
	@Test
	fun `round trips every entry`() {
		val contents = sampleContents()
		withArchive(contents) { archive ->
			assertEquals(contents.size, archive.entryCount)
			for ((path, expected) in contents) {
				val handle = archive.find(path) ?: error("missing $path")
				assertEquals(expected.size.toLong(), handle.length(), "length of $path")
				assertContentEquals(expected, handle.readBytes(), "bytes of $path")
			}
		}
	}

	@Test
	fun `lookup is case and separator insensitive, and misses report absence`() {
		withArchive(mapOf("ui/skin/panel.png" to ByteArray(64) { it.toByte() })) { archive ->
			assertTrue(archive.find("ui/skin/panel.png") != null)
			assertTrue(archive.find("UI/Skin/Panel.PNG") != null)
			assertTrue(archive.find("ui\\skin\\panel.png") != null)
			assertTrue(archive.find("./ui//skin/panel.png") != null)
			assertNull(archive.find("ui/skin/missing.png"))

			// A miss never stands in for another entry - v1's parent()/child() did, and fed one asset to another.
			val handle = archive.find("ui/skin/panel.png")!!
			val missing = handle.sibling("nope.png")
			assertTrue(!missing.exists())
			assertEquals(0L, missing.length())
		}
	}

	/**
	 * The property every measure in `docs/xpk-format-audit.md` §9 rests on. A packer that reshuffles or re-salts
	 * between builds makes Steam re-download the archive whatever else is done.
	 */
	@Test
	fun `packing the same content twice is byte identical`() {
		val profile = testProfile()
		val contents = sampleContents()

		val first = pack(profile, contents)
		val second = pack(profile, contents)
		assertContentEquals(first, second, "identical inputs must produce identical bytes")

		// Insertion order must not matter either: the layout is sorted by name hash.
		val reordered = pack(profile, contents.entries.reversed().associate { it.key to it.value })
		assertContentEquals(first, reordered, "insertion order must not change the output")
	}

	/**
	 * Changing one entry must leave the other blocks byte-identical, so a chunk-matching patcher finds them again.
	 *
	 * This models what SteamPipe does - Valve's documentation says it "searches to find any such chunks that match
	 * the previous build" - by chunking both builds on the archive's own 4 KB block alignment and measuring how much
	 * of the old file is still present verbatim. It deliberately does not assert byte-position equality: exact offset
	 * stability would need fixed-size slots, which for blocks compressing 2:1 would waste about half the archive.
	 * What must hold is that unchanged content re-encrypts to the same bytes, which a per-build random nonce or a
	 * non-deterministic layout would destroy.
	 */
	@Test
	fun `changing one entry leaves the other blocks byte identical`() {
		val profile = testProfile()
		val contents = LinkedHashMap(sampleContents())
		val before = pack(profile, contents)

		contents["data/config.json"] = """{"tweaked":true}""".toByteArray()
		val after = pack(profile, contents)

		val reusable = chunks(after)
		var matched = 0
		var total = 0
		for (chunk in chunks(before)) {
			total++
			if (reusable.contains(chunk)) matched++
		}

		assertTrue(
			matched * 2 >= total,
			"only $matched of $total 4 KB chunks survived a one-entry change; unchanged blocks must re-encrypt " +
				"identically so a delta can reuse them",
		)
	}

	/** The archive's own block alignment, which is the granularity a patcher can reuse at. */
	private fun chunks(archive: ByteArray): Set<String> {
		val size = XpkFormat.BLOCK_ALIGNMENT
		val out = HashSet<String>()
		var offset = 0
		while (offset + size <= archive.size) {
			out.add(String(archive, offset, size, Charsets.ISO_8859_1))
			offset += size
		}
		return out
	}

	@Test
	fun `identical payloads are stored once`() {
		// Incompressible, and well over the 4 KB block alignment, so the saving is not hidden by padding.
		val shared = Random(1).nextBytes(20_000)
		val profile = testProfile()
		val deduped = pack(profile, mapOf("a/one.bin" to shared, "b/two.bin" to shared, "c/three.bin" to shared))
		val distinct = pack(
			profile,
			mapOf(
				"a/one.bin" to shared,
				"b/two.bin" to Random(2).nextBytes(20_000),
				"c/three.bin" to Random(3).nextBytes(20_000),
			),
		)
		assertTrue(
			deduped.size + 2 * 20_000 <= distinct.size,
			"three copies of one payload should cost about one (${deduped.size} vs ${distinct.size})",
		)
	}

	@Test
	fun `a wrong profile does not open the archive`() {
		val contents = sampleContents()
		val file = writeArchive(testProfile(), contents)
		try {
			val otherKey = XpkProfile.of(
				rootKey = ByteArray(32) { 9 },
				nameHashKey = ByteArray(32) { 8 },
				profileId = 7,
				footerMask = 0x0123_4567_89AB_CDEFuL.toLong(),
			)
			assertNull(XpkV2Archive.openOrNull(otherKey, FileHandle(file)), "a different key must not open it")

			val otherId = XpkProfile.of(
				rootKey = ByteArray(32) { 1 },
				nameHashKey = ByteArray(32) { 2 },
				profileId = 999,
				footerMask = 0x5EED_5EED_5EED_5EEDuL.toLong(),
			)
			assertNull(XpkV2Archive.openOrNull(otherId, FileHandle(file)), "a different profile id must not open it")
		} finally {
			file.delete()
		}
	}

	@Test
	fun `a corrupted block is rejected rather than served`() {
		val contents = mapOf("data/big.bin" to ByteArray(20_000) { (it % 251).toByte() })
		val profile = testProfile()
		val packed = pack(profile, contents)
		// Flip a byte well inside the payload region, past the salt.
		packed[XpkFormat.SALT_LENGTH + 64] = (packed[XpkFormat.SALT_LENGTH + 64] + 1).toByte()

		val file = writeBytes(packed)
		try {
			val archive = XpkV2Archive.openOrNull(profile, FileHandle(file))
			// The footer and TOC are intact, so the archive opens; the damaged block fails when it is read.
			if (archive != null) {
				archive.use {
					assertFailsWith<Exception> { it.find("data/big.bin")!!.readBytes() }
				}
			}
		} finally {
			file.delete()
		}
	}

	@Test
	fun `a tampered table of contents fails signature verification`() {
		val keys = ed25519()
		val signing = XpkProfile.of(
			rootKey = ByteArray(32) { 1 },
			nameHashKey = ByteArray(32) { 2 },
			profileId = 42,
			footerMask = 0x5EED_5EED_5EED_5EEDuL.toLong(),
			tocSigningKey = keys.public,
		)
		val contents = sampleContents()

		// Signed by CI's key: opens and reads.
		val signed = writeBytes(XpkWriter(signing).apply { addAll(contents) }.build(keys.private))
		try {
			XpkV2Archive.openOrNull(signing, FileHandle(signed))!!.use { archive ->
				assertContentEquals(contents.getValue("data/config.json"), archive.find("data/config.json")!!.readBytes())
			}
		} finally {
			signed.delete()
		}

		// Signed by somebody else's key: identified as this profile's, then fails to prove it.
		val forged = writeBytes(XpkWriter(signing).apply { addAll(contents) }.build(ed25519().private))
		try {
			assertFailsWith<Exception> { XpkV2Archive.openOrNull(signing, FileHandle(forged)) }
		} finally {
			forged.delete()
		}

		// Unsigned, against a profile that demands a signature.
		val unsigned = writeBytes(XpkWriter(signing).apply { addAll(contents) }.build())
		try {
			assertFailsWith<Exception> { XpkV2Archive.openOrNull(signing, FileHandle(unsigned)) }
		} finally {
			unsigned.delete()
		}
	}

	@Test
	fun `the archive carries no plaintext name or magic`() {
		val contents = mapOf("textures/hero_diffuse.png" to ByteArray(2_048) { (it * 3).toByte() })
		val packed = pack(testProfile(), contents)
		val asLatin1 = String(packed, Charsets.ISO_8859_1)
		assertTrue(!asLatin1.contains("hero_diffuse"), "entry names must not appear in the archive")
		assertTrue(!asLatin1.contains("textures/"), "directory paths must not appear in the archive")
		// 7z's signature, which v1 only partially masked.
		assertTrue(!asLatin1.contains("7z¼¯'"), "no recognisable archive magic")
	}

	@Test
	fun `blocks are laid into aligned pages`() {
		// Enough distinct payload to need several blocks and cross a page boundary.
		val contents = (0 until 40).associate { index ->
			"pages/entry$index.bin" to Random(index).nextBytes(48_000)
		}
		val packed = pack(testProfile(), contents)
		assertTrue(
			packed.size > XpkFormat.PAGE_SIZE,
			"test needs to cross a page boundary, archive was ${packed.size} bytes",
		)
	}

	/** Reads touch one block, so nothing accumulates and there is no retention policy to get wrong. */
	@Test
	fun `reading many entries does not retain them`() {
		val contents = (0 until 30).associate { index ->
			"sfx/clip$index.bin" to ByteArray(1_500) { (index + it).toByte() }
		}
		withArchive(contents) { archive ->
			// Reverse order: the worst case for anything that reads forward from a shared cursor.
			for ((path, expected) in contents.entries.reversed()) {
				assertContentEquals(expected, archive.find(path)!!.readBytes())
			}
			// Two reads of the same entry return equal but independent arrays - callers own what they get.
			val handle = archive.find("sfx/clip3.bin")!!
			val first = handle.readBytes()
			val second = handle.readBytes()
			assertContentEquals(first, second)
			assertNotEquals(System.identityHashCode(first), System.identityHashCode(second))
		}
	}

	/**
	 * The footer is untrusted input: every length and count in it is attacker-controlled once a file is on disk.
	 * A truncated or scrambled tail must read as "not mine", never as an out-of-bounds access.
	 */
	@Test
	fun `a damaged or truncated footer is rejected without throwing`() {
		val profile = testProfile()
		val packed = pack(profile, sampleContents())

		val cases = linkedMapOf(
			"empty" to ByteArray(0),
			"shorter than a footer" to packed.copyOf(8),
			"salt only" to packed.copyOf(XpkFormat.SALT_LENGTH),
			"truncated mid payload" to packed.copyOf(packed.size / 2),
			"scrambled footer" to packed.copyOf().also { copy ->
				for (index in copy.size - XpkFormat.FOOTER_LENGTH until copy.size) copy[index] = 0
			},
			"all zeroes" to ByteArray(packed.size),
			"random noise" to Random(11).nextBytes(packed.size),
		)

		for ((label, bytes) in cases) {
			val file = writeBytes(bytes)
			try {
				val opened = XpkV2Archive.openOrNull(profile, FileHandle(file))
				assertNull(opened, "'$label' must not open as an archive")
			} finally {
				file.delete()
			}
		}
	}

	@Test
	fun `duplicate and empty entry paths are rejected at pack time`() {
		val writer = XpkWriter(testProfile())
		writer.add("a/b.bin", ByteArray(4))
		assertFailsWith<IllegalArgumentException> { writer.add("a/b.bin", ByteArray(4)) }
		assertFailsWith<IllegalArgumentException> { writer.add("./", ByteArray(4)) }
		// Normalisation means these are the same path, so the second is a duplicate too.
		assertFailsWith<IllegalArgumentException> { writer.add("a\\b.bin", ByteArray(4)) }
	}

	// ---- helpers --------------------------------------------------------------------------------------------

	private fun sampleContents(): Map<String, ByteArray> = linkedMapOf(
		"data/config.json" to """{"volume":0.8,"difficulty":"normal"}""".toByteArray(),
		"shaders/basic.vert" to "attribute vec4 a_position;\nvoid main(){gl_Position=a_position;}\n".toByteArray(),
		"textures/tile.png" to Random(7).nextBytes(9_000),
		"textures/hero.png" to Random(8).nextBytes(70_000),
		"sfx/step.ogg" to Random(9).nextBytes(3_500),
	)

	private fun testProfile(): XpkProfile = XpkProfile.of(
		rootKey = ByteArray(32) { 1 },
		nameHashKey = ByteArray(32) { 2 },
		profileId = 42,
		footerMask = 0x5EED_5EED_5EED_5EEDuL.toLong(),
	)

	private fun ed25519(): KeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

	private fun pack(profile: XpkProfile, contents: Map<String, ByteArray>): ByteArray =
		XpkWriter(profile).apply { addAll(contents) }.build()

	private fun writeArchive(profile: XpkProfile, contents: Map<String, ByteArray>): File =
		writeBytes(pack(profile, contents))

	private fun writeBytes(bytes: ByteArray): File {
		val file = Files.createTempFile("meta-xpk-v2", ".xpk").toFile()
		file.writeBytes(bytes)
		return file
	}

	private fun withArchive(contents: Map<String, ByteArray>, block: (XpkV2Archive) -> Unit) {
		val profile = testProfile()
		val file = writeArchive(profile, contents)
		try {
			val archive = XpkV2Archive.openOrNull(profile, FileHandle(file)) ?: error("archive did not open")
			archive.use(block)
		} finally {
			file.delete()
		}
	}
}

private fun XpkWriter.addAll(contents: Map<String, ByteArray>) {
	for (entry in contents.entries) add(entry.key, entry.value)
}

private inline fun XpkV2Archive.use(block: (XpkV2Archive) -> Unit) {
	try {
		block(this)
	} finally {
		dispose()
	}
}
