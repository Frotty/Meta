package de.fatox.meta.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Scenario tests for [ShakeModel] - the pure trauma-shake maths behind camera shake.
 */
class ShakeModelTest {

	private val tile = 32f
	private val maxPx = ShakeModel.MAX_TRANSLATION

	/** Peak px the audible floor alone produces - the guaranteed minimum for any blast you can hear. */
	private val floorPx = maxPx * ShakeModel.AUDIBLE_FLOOR * ShakeModel.AUDIBLE_FLOOR

	/** A simultaneous explosion: [tiles] tiles at [distInTiles] away. [audible] => each tile carries the floor. */
	private fun bomb(
		model: ShakeModel,
		tiles: Int,
		distInTiles: Float,
		audible: Boolean = true,
		perTile: Float = ShakeModel.EXPLOSION_NEAR_AMOUNT,
	) {
		val minFloor = if (audible) ShakeModel.AUDIBLE_FLOOR else 0f
		repeat(tiles) { model.addShakeAtDistance(perTile, distInTiles * tile, minFloor = minFloor) }
	}

	private fun simultaneousPeak(tiles: Int, distInTiles: Float, audible: Boolean = true): Float =
		ShakeModel().also { bomb(it, tiles, distInTiles, audible) }.peakTranslation

	@Test
	fun `no shake before anything happens`() {
		val model = ShakeModel()
		assertEquals(0f, model.trauma)
		assertEquals(0f, model.peakTranslation)
	}

	@Test
	fun `a single bomb right next to the listener is a solid bump`() {
		val peak = simultaneousPeak(tiles = 9, distInTiles = 1f)
		assertTrue(peak in 9f..maxPx, "adjacent single bomb peak was $peak px, expected 9..$maxPx")
	}

	@Test
	fun `a single bomb far away without floor is almost nothing`() {
		val peak = simultaneousPeak(tiles = 9, distInTiles = 30f, audible = false)
		assertTrue(peak < 0.7f, "far inaudible bomb peak was $peak px, expected near-zero")
	}

	@Test
	fun `far but audible blast still has a minimum floor`() {
		val peak = simultaneousPeak(tiles = 9, distInTiles = 18f, audible = true)
		assertTrue(peak >= floorPx - 0.01f, "far audible bomb peak was $peak px, expected >= floor $floorPx")
	}

	@Test
	fun `more simultaneous blasts are stronger than one`() {
		val oneBomb = simultaneousPeak(tiles = 9, distInTiles = 15f)
		val twelveBombs = simultaneousPeak(tiles = 9 * 12, distInTiles = 15f)
		assertTrue(twelveBombs > oneBomb, "12 bombs should out-shake one bomb")
	}

	@Test
	fun `trauma clamps to full but bounded`() {
		val model = ShakeModel()
		model.addShake(100f)
		assertEquals(1f, model.trauma, 0.0001f)
		assertEquals(maxPx, model.peakTranslation, 0.001f)
	}

	@Test
	fun `chain spread is weaker than simultaneous`() {
		val tilesCount = 9
		val distInTiles = 1f

		val simultaneous = simultaneousPeak(tilesCount, distInTiles)

		val chained = ShakeModel()
		var chainedPeak = 0f
		repeat(tilesCount) {
			chained.addShakeAtDistance(ShakeModel.EXPLOSION_NEAR_AMOUNT, distInTiles * tile, minFloor = ShakeModel.AUDIBLE_FLOOR)
			if (chained.peakTranslation > chainedPeak) chainedPeak = chained.peakTranslation
			chained.update(delta = 1f / 128f)
		}
		assertTrue(chainedPeak < simultaneous, "spread chain ($chainedPeak) should rumble less than simultaneous ($simultaneous)")
	}

	@Test
	fun `audible range scales with window size`() {
		val base = 600f
		assertEquals(600f, audibleShakeRange(base, screenWidthPx = 800), 0.001f)
		assertEquals(1280f, audibleShakeRange(base, screenWidthPx = 2560), 0.001f)
	}
}
