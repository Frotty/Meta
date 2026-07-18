package de.fatox.meta.api.ui

import de.fatox.meta.ui.MetaButtonTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MetaToastSpecTest {
	@Test
	fun `notification presets auto dismiss while primary and error presets persist`() {
		assertEquals(UIManager.DEFAULT_TOAST_SECONDS, MetaToastSpec.notification("Saved").autoDismissSeconds)
		assertEquals(UIManager.IMPORTANT_TOAST_SECONDS,
			MetaToastSpec.notification("Careful", MetaToastType.WARNING).autoDismissSeconds)
		assertNull(MetaToastSpec.error("Connection failed").autoDismissSeconds)
		assertNull(MetaToastSpec.invite("Join?", onAccept = {}).autoDismissSeconds)
	}

	@Test
	fun `error and invite presets provide sane dismissal and action defaults`() {
		var accepted = false
		val error = MetaToastSpec.error("Connection failed")
		val invite = MetaToastSpec.invite("Join Frotty?", onAccept = { accepted = true })

		assertEquals(MetaToastType.ERROR, error.type)
		assertEquals("Dismiss", error.dismissLabel)
		assertEquals(MetaToastType.PRIMARY, invite.type)
		assertEquals("Accept", invite.primaryAction?.label)
		assertEquals(MetaButtonTier.PRIMARY, invite.primaryAction?.tier)
		invite.primaryAction?.action?.invoke()
		assertTrue(accepted)
	}
}
