package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.ObjectIntMap
import de.fatox.meta.api.extensions.forEachEntryReentrant
import de.fatox.meta.ui.components.MetaGlyphSet
import java.util.EnumMap

/**
 * Kenney's input-prompt faces, rasterized at exactly the size they are drawn at.
 *
 * <p>The art is Kenney's - hand-drawn keycaps and buttons, CC0, bundled under `ui/prompts/` - and this only decides
 * how it reaches the screen. Every glyph in these faces occupies the same cell: a square half an em tall, sitting on
 * the baseline. So a face is rasterized at twice the cell's physical pixel height and each glyph's bitmap is drawn
 * 1:1 at its physical size, which is what keeps the drawn lines as crisp as they are in the vector art. The old way
 * - one large face, scaled down by the batch - is what made them soft.
 *
 * <p>Faces are opened on first use and rasterize incrementally into their own atlas pages, so a game that only
 * shows keyboard prompts never opens the PlayStation face. Everything here runs on the GL thread.
 */
internal object MetaInputGlyphFaces {
	private const val BASE = "ui/prompts/"
	private const val PAGE_SIZE = 1024

	private class Face(val set: MetaGlyphSet) {
		val generator = FreeTypeFontGenerator(Gdx.files.classpath("$BASE${set.fileName}.ttf"))
		val codepoints = ObjectIntMap<String>()
		/** One glyph rasterized up front: a face generated with none has no page, and libGDX refuses to make it. */
		val seed: String = String(Character.toChars(loadIndex(set, codepoints)))
		val bySize = IntMap<BitmapFont>()
		/** Per em size, the codepoints already rasterized and uploaded. */
		val uploaded = IntMap<IntSet>()
		val packer = PixmapPacker(PAGE_SIZE, PAGE_SIZE, Pixmap.Format.RGBA8888, 2, false)
	}

	private val faces = EnumMap<MetaGlyphSet, Face>(MetaGlyphSet::class.java)

	/**
	 * How many expensive steps have run: a face opened, a font generated at a new size, a glyph rasterized and uploaded.
	 * A test seam - after a prewarm, drawing must not move it.
	 */
	internal var work = 0
		private set
	private val region = TextureRegion()

	private fun face(set: MetaGlyphSet): Face = faces.getOrPut(set) {
		work++
		Face(set)
	}

	/** Whether [set] has a glyph called [name]. */
	fun has(set: MetaGlyphSet, name: String): Boolean = face(set).codepoints.containsKey(name)

	/**
	 * The bitmap of glyph [name] rasterized so its cell is [cellPixels] physical pixels tall, or null when the face
	 * has no such glyph. The returned region is shared scratch: draw it before asking for another.
	 */
	fun region(set: MetaGlyphSet, name: String, cellPixels: Int): TextureRegion? {
		val face = face(set)
		val codepoint = face.codepoints.get(name, -1)
		if (codepoint < 0) return null
		// The cell is half an em, so the em is twice it.
		val emPixels = (cellPixels * 2).coerceAtLeast(8)
		val font = fontFor(face, emPixels)
		val glyph = font.data.getGlyph(codepoint.toChar()) ?: return null
		// Incremental faces only upload new glyph pixels when a layout asks for them; a direct lookup rasterizes into
		// the packer's pixmap and stops there, so the first draw would sample a page that has not seen the glyph.
		val seen = face.uploaded.get(emPixels) ?: IntSet().also { face.uploaded.put(emPixels, it) }
		if (seen.add(codepoint)) {
			work++
			face.packer.updateTextureRegions(font.regions, Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, false)
		}
		if (glyph.width == 0 || glyph.height == 0) return null
		region.texture = font.getRegion(glyph.page).texture
		region.setRegion(glyph.srcX, glyph.srcY, glyph.width, glyph.height)
		return region
	}

	/**
	 * The share of a rasterized glyph's bitmap that is ink. A test seam: it is how the cut-outs are held without a
	 * frame buffer - a glyph whose holes were filled in by the winding bug is measurably more ink.
	 */
	internal fun inkCoverage(set: MetaGlyphSet, name: String, cellPixels: Int): Float {
		val drawn = region(set, name, cellPixels) ?: return 0f
		val face = face(set)
		val pages = face.packer.pages
		var pixmap: Pixmap? = null
		for (i in 0 until pages.size) if (pages[i].texture === drawn.texture) pixmap = pages[i].pixmap
		val source = pixmap ?: return 0f
		var ink = 0
		val total = drawn.regionWidth * drawn.regionHeight
		for (y in 0 until drawn.regionHeight) for (x in 0 until drawn.regionWidth) {
			if ((source.getPixel(drawn.regionX + x, drawn.regionY + y) and 0xFF) > 128) ink++
		}
		return ink.toFloat() / total
	}

	private fun fontFor(face: Face, emPixels: Int): BitmapFont {
		face.bySize.get(emPixels)?.let { return it }
		val params = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
			size = emPixels
			incremental = true
			packer = face.packer
			// Drawn 1:1, so nearest would be exact - but the batch may still land a glyph on a half pixel under an
			// odd UI scale, and linear keeps that edge smooth instead of dropping a row.
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			hinting = FreeTypeFontGenerator.Hinting.None
			characters = face.seed
		}
		val font = face.generator.generateFont(params)
		work++
		face.bySize.put(emPixels, font)
		return font
	}

	/** Fills [map] from the face's name table and returns its first codepoint. */
	private fun loadIndex(set: MetaGlyphSet, map: ObjectIntMap<String>): Int {
		var first = -1
		val lines = Gdx.files.classpath("$BASE${set.fileName}.tsv").readString("UTF-8").lines()
		for (index in lines.indices) {
			val line = lines[index]
			if (line.isBlank() || line.startsWith("#")) continue
			val tab = line.indexOf('\t')
			if (tab <= 0) continue
			val codepoint = line.substring(tab + 1).trim().toInt(16)
			if (first < 0) first = codepoint
			map.put(line.substring(0, tab), codepoint)
		}
		check(first >= 0) { "${set.fileName}.tsv lists no glyphs" }
		return first
	}

	/** Releases every face. The next request opens them again. */
	fun dispose() {
		val sets = MetaGlyphSet.entries
		for (index in sets.indices) {
			val face = faces[sets[index]] ?: continue
			face.bySize.forEachEntryReentrant { _, font -> font.dispose() }
			// The fonts share the packer's pages rather than owning them, so the pages go with the packer.
			val pages = face.packer.pages
			for (page in 0 until pages.size) pages[page].texture?.dispose()
			face.packer.dispose()
			face.generator.dispose()
		}
		faces.clear()
	}
}
