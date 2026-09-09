package de.fatox.meta.assets

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StagedTextureUploadPolicyTest {
	@Test
	fun `asset updates keep stepping only while their time budget remains`() {
		val startedAt = 5_000_000L

		assertTrue(AssetUpdateBudget.hasTimeRemaining(startedAt, startedAt + 999_999L, 1))
		assertFalse(AssetUpdateBudget.hasTimeRemaining(startedAt, startedAt + 1_000_000L, 1))
		assertFalse(AssetUpdateBudget.hasTimeRemaining(startedAt, startedAt, 0))
	}

	@Test
	fun `large atlas pages are staged while small textures keep the normal path`() {
		assertTrue(StagedTextureUploadPolicy.shouldStage(2048, 2048))
		assertTrue(StagedTextureUploadPolicy.shouldStage(512, 512))
		assertFalse(StagedTextureUploadPolicy.shouldStage(511, 512))
	}

	@Test
	fun `row batches stay within the byte budget when a row fits`() {
		assertEquals(
			64,
			StagedTextureUploadPolicy.rowsForBudget(
				rowBytes = 2048 * 4,
				remainingRows = 2048,
				budgetBytes = StagedTextureUploadPolicy.MIN_BYTES_PER_STEP,
			),
		)
	}

	@Test
	fun `at least one row progresses when a row exceeds the budget`() {
		assertEquals(
			1,
			StagedTextureUploadPolicy.rowsForBudget(
				rowBytes = StagedTextureUploadPolicy.MIN_BYTES_PER_STEP * 2,
				remainingRows = 3,
				budgetBytes = StagedTextureUploadPolicy.MIN_BYTES_PER_STEP,
			),
		)
	}

	@Test
	fun `texture transfer size scales with the frame budget and stays bounded`() {
		assertEquals(StagedTextureUploadPolicy.MIN_BYTES_PER_STEP, StagedTextureUploadPolicy.bytesForBudget(1))
		assertEquals(StagedTextureUploadPolicy.MAX_BYTES_PER_STEP, StagedTextureUploadPolicy.bytesForBudget(8))
		assertEquals(StagedTextureUploadPolicy.MAX_BYTES_PER_STEP, StagedTextureUploadPolicy.bytesForBudget(1_000))
	}
}
