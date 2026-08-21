package de.fatox.meta.graphics.font

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import de.fatox.meta.api.graphics.PhysicalPixelDensityFont

/**
 * Creates Meta's final, asset-independent text fallback. The glyph pixels live in code and the tiny atlas is built
 * directly on the GL thread, so this remains available even when a consumer's packaging step drops every resource.
 */
internal fun createEmergencyBitmapFont(size: Int, physicalPixelDensity: Float): BitmapFont {
	val data = createEmergencyBitmapFontData()
	val pixmap = Pixmap(ATLAS_WIDTH, ATLAS_HEIGHT, Pixmap.Format.RGBA8888)
	pixmap.setColor(Color.WHITE)
	for (index in EMERGENCY_GLYPH_PATTERNS.indices) {
		val cellX = index % ATLAS_COLUMNS * CELL_WIDTH
		val cellY = index / ATLAS_COLUMNS * CELL_HEIGHT
		val pattern = EMERGENCY_GLYPH_PATTERNS[index]
		for (row in 0 until GLYPH_HEIGHT) {
			val rowBits = ((pattern ushr ((GLYPH_HEIGHT - 1 - row) * 8)) and 0x1fL).toInt()
			for (column in 0 until GLYPH_WIDTH) {
				if (rowBits and (1 shl (GLYPH_WIDTH - 1 - column)) != 0) {
					pixmap.drawPixel(cellX + column, cellY + row)
				}
			}
		}
	}
	val texture = try {
		Texture(pixmap)
	} finally {
		pixmap.dispose()
	}
	texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
	val font = try {
		EmergencyBitmapFont(data, TextureRegion(texture), physicalPixelDensity)
	} catch (failure: RuntimeException) {
		texture.dispose()
		throw failure
	}
	font.setOwnsTexture(true)
	val capHeight = font.capHeight
	if (capHeight > 0f) font.data.setScale(size / capHeight)
	font.setUseIntegerPositions(false)
	return font
}

internal fun createEmergencyBitmapFontData(): BitmapFont.BitmapFontData {
	check(EMERGENCY_GLYPH_CHARS.length + 1 == EMERGENCY_GLYPH_PATTERNS.size)
	check(EMERGENCY_GLYPH_PATTERNS.size <= ATLAS_COLUMNS * ATLAS_ROWS)
	val data = BitmapFont.BitmapFontData().apply {
		lineHeight = CELL_HEIGHT.toFloat()
		capHeight = GLYPH_HEIGHT.toFloat()
		xHeight = GLYPH_HEIGHT.toFloat()
		ascent = 0f
		descent = 0f
		down = -CELL_HEIGHT.toFloat()
		spaceXadvance = SPACE_ADVANCE.toFloat()
	}
	for (index in EMERGENCY_GLYPH_CHARS.indices) {
		val character = EMERGENCY_GLYPH_CHARS[index]
		data.setGlyph(character.code, createGlyph(character.code, index))
		if (character in 'A'..'Z') {
			val lowercase = character.lowercaseChar()
			data.setGlyph(lowercase.code, createGlyph(lowercase.code, index))
		}
	}
	data.setGlyph(' '.code, BitmapFont.Glyph().apply {
		id = ' '.code
		xadvance = SPACE_ADVANCE
	})
	val missing = createGlyph(TOFU_CODE_POINT, EMERGENCY_GLYPH_PATTERNS.lastIndex)
	data.setGlyph(TOFU_CODE_POINT, missing)
	data.missingGlyph = missing
	return data
}

private fun createGlyph(codePoint: Int, patternIndex: Int): BitmapFont.Glyph {
	return BitmapFont.Glyph().apply {
		id = codePoint
		srcX = patternIndex % ATLAS_COLUMNS * CELL_WIDTH
		srcY = patternIndex / ATLAS_COLUMNS * CELL_HEIGHT
		width = GLYPH_WIDTH
		height = GLYPH_HEIGHT
		xadvance = CELL_WIDTH
	}
}

private class EmergencyBitmapFont(
	data: BitmapFontData,
	region: TextureRegion,
	override val physicalPixelDensity: Float,
) : BitmapFont(data, region, false), PhysicalPixelDensityFont

private const val GLYPH_WIDTH = 5
private const val GLYPH_HEIGHT = 7
private const val CELL_WIDTH = 6
private const val CELL_HEIGHT = 8
private const val SPACE_ADVANCE = 4
private const val ATLAS_COLUMNS = 8
private const val ATLAS_ROWS = 5
private const val TOFU_CODE_POINT = 0xfffd
private const val EMERGENCY_GLYPH_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
private const val ATLAS_WIDTH = ATLAS_COLUMNS * CELL_WIDTH
private const val ATLAS_HEIGHT = ATLAS_ROWS * CELL_HEIGHT

/** Seven 5-bit rows packed most-significant row first. Final entry is the missing-glyph box. */
private val EMERGENCY_GLYPH_PATTERNS = longArrayOf(
	0x0e11111f111111, 0x1e11111e11111e, 0x0e11101010110e, 0x1e11111111111e,
	0x1f10101e10101f, 0x1f10101e101010, 0x0e11101711110f, 0x1111111f111111,
	0x1f04040404041f, 0x0702020212120c, 0x11121418141211, 0x1010101010101f,
	0x111b1515111111, 0x11191513111111, 0x0e11111111110e, 0x1e11111e101010,
	0x0e11111115120d, 0x1e11111e141211, 0x0f10100e01011e, 0x1f040404040404,
	0x1111111111110e, 0x11111111110a04, 0x11111115151b11, 0x11110a040a1111,
	0x11110a04040404, 0x1f01020408101f,
	0x0e11131519110e, 0x040c140404041f, 0x0e11010204081f, 0x1e01010e01011e,
	0x02060a121f0202, 0x1f10101e01011e, 0x0e10101e11110e, 0x1f010204080808,
	0x0e11110e11110e, 0x0e11110f01010e,
	0x1f11151515111f,
)
