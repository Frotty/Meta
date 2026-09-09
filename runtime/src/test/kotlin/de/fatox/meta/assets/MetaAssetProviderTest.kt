package de.fatox.meta.assets

import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader
import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.GdxRuntimeException
import de.fatox.meta.assets.xpk.XpkProfile
import de.fatox.meta.assets.xpk.XpkWriter
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.test.GdxTestEnvironment
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.system.measureTimeMillis

class MetaAssetProviderTest {
	/**
	 * A loose file and a packed entry naming the same asset must be reported, not silently ranked.
	 *
	 * `cacheFile` already refuses an overlap between loose files and v1 archives. Resolving loose-versus-v2 with an
	 * `?:` instead would have made one lookup path loud and another quiet about the same mistake, so a stale pack
	 * could serve different bytes depending on which route reached it.
	 */
	@Test
	fun `an asset in both a loose folder and a packed archive is reported`() {
		val root = Files.createTempDirectory("meta-overlap").toFile()
		try {
			val loose = File(root, "assets/data").apply { mkdirs() }
			File(loose, "shared.bin").writeBytes(ByteArray(16) { 1 })

			val profile = XpkProfile.of(
				rootKey = ByteArray(32) { 1 },
				nameHashKey = ByteArray(32) { 2 },
				profileId = 42,
				footerMask = 0x5EED_5EED_5EED_5EEDuL.toLong(),
			)
			MetaInject.global { singleton(profile) }
			try {
				val packs = File(root, "packs").apply { mkdirs() }
				File(packs, "content.xpk").writeBytes(
					XpkWriter(profile).apply { add("data/shared.bin", ByteArray(16) { 2 }) }.build(),
				)

				val provider = MetaAssetProvider()
				assertTrue(provider.loadRawAssetsFromFolder(FileHandle(File(root, "assets"))))
				assertTrue(provider.loadPackedAssetsFromFolder(FileHandle(packs)))

				// Every spelling must reach both sources. While the indexed key folded only slashes and case while
				// the packed one also stripped empty and `.` segments, `./data//shared.bin` missed the index, hit
				// the archive, and returned the packed entry instead of reporting the clash.
				for (spelling in listOf("data/shared.bin", "./data//shared.bin", "Data\\Shared.BIN")) {
					val failure = assertFailsWith<GdxRuntimeException>("spelling '$spelling' should have clashed") {
						provider.getResource(spelling, FileHandle::class.java)
					}
					assertTrue(
						failure.message.orEmpty().contains("packed archive"),
						"the overlap should name both sources, was: ${failure.message}",
					)
				}
				provider.dispose()
			} finally {
				MetaInject.global(clear = true) {}
			}
		} finally {
			root.deleteRecursively()
		}
	}

	@Test
	fun `texture atlas dependency discovery uses an asynchronous loader`() {
		assertTrue(
			AsynchronousAssetLoader::class.java.isAssignableFrom(MetaTextureAtlasLoader::class.java),
			"Atlas parsing and XPK decompression must not run in AssetManager.update on the render thread",
		)
		assertTrue(
			AsynchronousAssetLoader::class.java.isAssignableFrom(MetaTextureLoader::class.java),
			"Texture decoding and mip generation must stay on AssetManager's worker",
		)
	}

	@Test
	fun `queued assets complete through incremental updates`() {
		val provider = MetaAssetProvider()
		Thread { provider.load("meta-icon-error.png", Pixmap::class.java) }.apply {
			start()
			join()
		}
		assertEquals(0f, provider.progress, "load() must queue rather than finish the asset synchronously")
		assertEquals(false, provider.update(0), "A zero budget must poll without advancing AssetManager")
		assertEquals(0f, provider.progress, "A zero budget must not start queued work")

		var complete = false
		var attempts = 0
		while (!complete && attempts++ < MAX_UPDATE_ATTEMPTS) {
			complete = provider.update(UPDATE_BUDGET_MS)
			if (!complete) Thread.sleep(1)
		}

		assertTrue(complete, "Asset queue did not complete")
		assertEquals(1f, provider.progress)
		provider.getResource("meta-icon-error.png", Pixmap::class.java)
		provider.dispose()
	}

	@Test
	fun `a generous update budget drains multiple completed loading steps`() {
		val provider = MetaAssetProvider()
		val manager = assetManagerOf(provider)
		manager.setLoader(ImmediateAsset::class.java, ImmediateAssetLoader(provider.MetaFileHandleResolver()))
		provider.load("first.asset", ImmediateAsset::class.java)
		provider.load("second.asset", ImmediateAsset::class.java)
		provider.load("third.asset", ImmediateAsset::class.java)

		assertTrue(provider.update(1_000), "The provider should keep advancing work inside the supplied budget")
		provider.dispose()
	}

	@Test
	fun `an unfinished asynchronous loader returns without spinning through the budget`() {
		val started = CountDownLatch(1)
		val release = CountDownLatch(1)
		val provider = MetaAssetProvider()
		val manager = assetManagerOf(provider)
		manager.setLoader(BlockedAsset::class.java, BlockedAssetLoader(provider.MetaFileHandleResolver(), started, release))
		try {
			provider.load("blocked.asset", BlockedAsset::class.java)
			provider.update(1)
			assertTrue(started.await(2, TimeUnit.SECONDS), "The asynchronous loader did not start")

			val elapsed = measureTimeMillis { assertEquals(false, provider.update(500)) }

			assertTrue(elapsed < 100, "A no-progress poll burned ${elapsed}ms of the render-thread budget")
		} finally {
			release.countDown()
			provider.dispose()
		}
	}

	@Test
	fun `lazy retrieval still loads a single unqueued asset`() {
		val provider = MetaAssetProvider()
		provider.getResource("meta-icon-error.png", Pixmap::class.java)
		assertEquals(1f, provider.progress)
		provider.dispose()
	}

	@Test
	fun `raw asset lookup ignores filename case and separator style`() {
		val root = FileHandle.tempDirectory("meta-case-assets")
		val font = root.child("Fonts").child("Oxanium-SemiBold.TTF")
		font.parent().mkdirs()
		font.writeString("test", false)
		val provider = MetaAssetProvider()
		try {
			assertTrue(provider.loadRawAssetsFromFolder(root))
			assertTrue(provider.getResource("fonts\\oxanium-semibold.ttf", FileHandle::class.java).exists())
		} finally {
			provider.dispose()
			root.deleteDirectory()
		}
	}

	@Test
	fun `asset lookup keys normalize separators and case without changing the source path`() {
		assertEquals("fonts/montserrat.ttf", assetPathKey("Fonts\\Montserrat.TTF"))
	}

	companion object {
		private const val UPDATE_BUDGET_MS = 1
		private const val MAX_UPDATE_ATTEMPTS = 1_000

		@JvmStatic
		@BeforeAll
		fun initializeGdx() = GdxTestEnvironment.ensure()
	}

	private class BlockedAsset
	private class ImmediateAsset

	private class ImmediateAssetLoader(
		resolver: FileHandleResolver,
	) : SynchronousAssetLoader<ImmediateAsset, AssetLoaderParameters<ImmediateAsset>>(resolver) {
		override fun getDependencies(
			fileName: String,
			file: FileHandle,
			parameter: AssetLoaderParameters<ImmediateAsset>?,
		): com.badlogic.gdx.utils.Array<AssetDescriptor<*>>? = null

		override fun load(
			manager: AssetManager,
			fileName: String,
			file: FileHandle,
			parameter: AssetLoaderParameters<ImmediateAsset>?,
		): ImmediateAsset = ImmediateAsset()
	}

	private class BlockedAssetLoader(
		resolver: FileHandleResolver,
		private val started: CountDownLatch,
		private val release: CountDownLatch,
	) : AsynchronousAssetLoader<BlockedAsset, AssetLoaderParameters<BlockedAsset>>(resolver) {
		override fun getDependencies(
			fileName: String,
			file: FileHandle,
			parameter: AssetLoaderParameters<BlockedAsset>?,
		): com.badlogic.gdx.utils.Array<AssetDescriptor<*>>? = null

		override fun loadAsync(
			manager: AssetManager,
			fileName: String,
			file: FileHandle,
			parameter: AssetLoaderParameters<BlockedAsset>?,
		) {
			started.countDown()
			release.await()
		}

		override fun loadSync(
			manager: AssetManager,
			fileName: String,
			file: FileHandle,
			parameter: AssetLoaderParameters<BlockedAsset>?,
		): BlockedAsset = BlockedAsset()
	}

	private fun assetManagerOf(provider: MetaAssetProvider): AssetManager {
		val field = MetaAssetProvider::class.java.getDeclaredField("assetManager").apply { isAccessible = true }
		return field.get(provider) as AssetManager
	}
}
