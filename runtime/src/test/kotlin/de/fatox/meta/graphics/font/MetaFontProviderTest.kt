package de.fatox.meta.graphics.font

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.test.GdxTestEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class MetaFontProviderTest {
	private var assets: MetaAssetProvider? = null

	@BeforeTest
	fun setUp() {
		GdxTestEnvironment.ensure()
		global(clear = true) {}
	}

	@AfterTest
	fun tearDown() {
		assets?.dispose()
		assets = null
		global(clear = true) {}
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

	@Test
	fun `missing application fonts fall back to bundled Meta faces`() {
		val provider = createProvider(
			FontInfo(
				"missing-regular.ttf",
				"missing-bold.ttf",
				"missing-mono.ttf",
				"missing-icons.ttf",
			),
		)

		provider.dispose()
	}

	@Test
	fun `invalid application fonts fall back to bundled Meta faces`() {
		val provider = createProvider(
			FontInfo(
				"invalid-font.ttf",
				"invalid-font.ttf",
				"invalid-font.ttf",
				"invalid-font.ttf",
			),
		)

		provider.dispose()
	}

	@Test
	fun `provider remains constructible when application and bundled fonts are absent`() {
		val fontInfo = FontInfo(
			"missing-regular.ttf",
			"missing-bold.ttf",
			"missing-mono.ttf",
			"missing-icons.ttf",
		)
		global(clear = true) {
			singleton<AssetProvider> { MissingFileAssetProvider }
			singleton { fontInfo }
		}

		MetaFontProvider().dispose()
	}

	@Test
	fun `bundled fallback faces are packaged and readable`() {
		val paths = arrayOf(
			FontInfo.DEFAULT_REGULAR_FONT_PATH,
			FontInfo.DEFAULT_BOLD_FONT_PATH,
			FontInfo.DEFAULT_MONO_FONT_PATH,
			FontInfo.DEFAULT_ICON_FONT_PATH,
		)

		for (path in paths) {
			val handle = Gdx.files.internal(path)
			assertTrue(handle.exists(), "Missing bundled fallback font: $path")
			assertTrue(handle.length() > 0L, "Empty bundled fallback font: $path")
		}
	}

	private fun createProvider(fontInfo: FontInfo): MetaFontProvider {
		val assetProvider = MetaAssetProvider()
		assets = assetProvider
		global(clear = true) {
			singleton<AssetProvider> { assetProvider }
			singleton { fontInfo }
		}
		return MetaFontProvider()
	}
}

private object MissingFileAssetProvider : AssetProvider {
	override fun loadPackedAssetsFromFolder(folder: FileHandle): Boolean = false
	override fun loadRawAssetsFromFolder(folder: FileHandle): Boolean = false
	override fun <T : Any> load(name: String, type: Class<T>) = Unit

	override fun <T : Any> getResource(fileName: String, type: Class<T>, index: Int): T {
		return type.cast(Gdx.files.internal("missing-font-test/$fileName"))
	}

	override fun getDrawable(name: String): Drawable = error("Not used")
	override fun finish() = Unit
	override fun loadAnimationFrames(baseName: String, frames: Int): Array<out TextureRegion> = Array()
}
