package de.fatox.meta.api

import com.badlogic.gdx.Files
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SplashLoadingPolicyTest {
	@Test
	fun `fast frames receive bounded loading time`() {
		assertEquals(8, SplashLoadingPolicy.updateBudgetMillis(1f / 120f))
		assertEquals(8, SplashLoadingPolicy.updateBudgetMillis(0f))
	}

	@Test
	fun `healthy refresh frames advance but slow frames recover without more work`() {
		assertEquals(8, SplashLoadingPolicy.updateBudgetMillis(1f / 60f))
		assertEquals(0, SplashLoadingPolicy.updateBudgetMillis(1f / 30f))
	}

	@Test
	fun `fade easing is clamped and smooth`() {
		assertEquals(0f, SplashLoadingPolicy.smoothStep(-1f))
		assertEquals(0.5f, SplashLoadingPolicy.smoothStep(0.5f))
		assertEquals(1f, SplashLoadingPolicy.smoothStep(2f))
	}

	@Test
	fun `spinner texture only paints the circular ring`() {
		val center = 31
		assertEquals(0f, SplashRingTexturePainter.alphaAt(center, center, 64))
		assertEquals(0f, SplashRingTexturePainter.alphaAt(4, 4, 64))
		assertTrue(SplashRingTexturePainter.alphaAt(center, 9, 64) > 0f)
	}

	@Test
	fun `spinner texture has a soft highlighted arc`() {
		val topArc = SplashRingTexturePainter.alphaAt(26, 10, 64)
		val sideRing = SplashRingTexturePainter.alphaAt(54, 31, 64)
		assertTrue(topArc > sideRing)
		assertTrue(sideRing > 0f)
	}

	@Test
	fun `presentation reports meaningful work throughout startup`() {
		val presentation = SplashPresentation(
			startingStatus = "start",
			preparationStatus = "content",
			queueStatus = "queue",
			assetStatus = "assets",
			interfaceStatus = "interface",
			applicationStatus = "application",
			readyStatus = "ready",
		)

		assertEquals("start", presentation.statusFor(SplashPhase.FADE_IN))
		assertEquals("content", presentation.statusFor(SplashPhase.PREPARING))
		assertEquals("queue", presentation.statusFor(SplashPhase.QUEUEING))
		assertEquals("assets", presentation.statusFor(SplashPhase.LOADING))
		assertEquals("interface", presentation.statusFor(SplashPhase.UI_LOADING))
		assertEquals("application", presentation.statusFor(SplashPhase.APP_LOADING))
		assertEquals("ready", presentation.statusFor(SplashPhase.HOLD))
		assertEquals("ready", presentation.statusFor(SplashPhase.FADE_OUT))
	}

	@Test
	fun `splash fonts can be configured independently`() {
		val fonts = SplashFontConfiguration(
			title = SplashFont("branding/title.ttf", Files.FileType.Classpath),
			body = SplashFont("branding/body.ttf", Files.FileType.Local),
		)

		assertEquals("branding/title.ttf", fonts.title?.path)
		assertEquals(Files.FileType.Classpath, fonts.title?.fileType)
		assertEquals("branding/body.ttf", fonts.body?.path)
		assertEquals(Files.FileType.Local, fonts.body?.fileType)
	}

	@Test
	fun `splash fonts default to bundled bitmap fonts`() {
		val fonts = SplashFontConfiguration()

		assertEquals(null, fonts.title)
		assertEquals(null, fonts.body)
	}

	@Test
	fun `splash transition exposes stable bootstrap defaults and validates geometry`() {
		val transition = SplashTransitionConfiguration()

		assertEquals(860, transition.bootstrapWidth)
		assertEquals(320, transition.bootstrapHeight)
		assertFailsWith<IllegalArgumentException> {
			SplashTransitionConfiguration(bootstrapWidth = 0)
		}
	}
}
