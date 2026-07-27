package de.fatox.meta.assets

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StagedTextureUploadPolicyTest {
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
				budgetBytes = StagedTextureUploadPolicy.MAX_BYTES_PER_UPDATE,
			),
		)
	}

	@Test
	fun `at least one row progresses when a row exceeds the budget`() {
		assertEquals(
			1,
			StagedTextureUploadPolicy.rowsForBudget(
				rowBytes = StagedTextureUploadPolicy.MAX_BYTES_PER_UPDATE * 2,
				remainingRows = 3,
				budgetBytes = StagedTextureUploadPolicy.MAX_BYTES_PER_UPDATE,
			),
		)
	}
}
