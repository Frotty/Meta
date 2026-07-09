package de.fatox.meta.graphics.font

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import de.fatox.meta.test.GdxTestEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class MetaFontProviderTest {
	@BeforeTest
	fun setUp() {
		GdxTestEnvironment.ensure()
	}

	@Test
	fun `remix icon font can generate metrics from icon glyphs`() {
		val measureChars = Character.toChars(0xEA13)
		val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
			incremental = true
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			hinting = FreeTypeFontGenerator.Hinting.Slight
			kerning = true
			size = 18
			characters = String(measureChars)
		}
		val data = FreeTypeFontGenerator.FreeTypeBitmapFontData().apply {
			xChars = measureChars
			capChars = measureChars
		}

		val failure = assertFailsWith<NullPointerException> {
			FreeTypeFontGenerator(Gdx.files.internal(FontInfo.DEFAULT_ICON_FONT_PATH))
				.generateData(parameter, data)
		}

		assertTrue(failure.message.orEmpty().contains("Gdx.gl"))
	}
}
