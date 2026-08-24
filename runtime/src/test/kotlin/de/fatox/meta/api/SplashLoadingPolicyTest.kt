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
	fun `a panel title does not scale with the window`() {
		// The panel's layout is built around a known size — mark box, gaps, baselines are all fixed — so scaling the
		// title would break it.
		assertEquals(
			SplashTitleSizing.forWindow(720, SplashStyle.PANEL),
			SplashTitleSizing.forWindow(1600, SplashStyle.PANEL),
		)
	}

	@Test
	fun `a quiet title scales with the window`() {
		// The symptom that prompted this: a fixed 25 px title in a 2560x1600 window is a caption, not a title.
		val small = SplashTitleSizing.forWindow(720, SplashStyle.QUIET)
		val large = SplashTitleSizing.forWindow(1600, SplashStyle.QUIET)
		assertTrue(large > small, "a larger window did not get a larger title: $small then $large")
	}

	@Test
	fun `a quiet title stays readable and stays sane`() {
		// Both ends matter. A 200-pixel-tall window must not get an unreadable title, and a wall-sized one must not
		// get a title that does not fit on it.
		assertTrue(SplashTitleSizing.forWindow(1, SplashStyle.QUIET) >= 28)
		assertTrue(SplashTitleSizing.forWindow(20_000, SplashStyle.QUIET) <= 180)
	}

	@Test
	fun `the panel fills the window unless it is a banner`() {
		val bounds = SplashPanelGeometry.bounds(1920, 1080, SplashTransitionConfiguration())
		assertEquals(listOf(0f, 0f, 1920f, 1080f), bounds.toList())
	}

	@Test
	fun `a banner is centred at its configured size`() {
		val transition = SplashTransitionConfiguration(bootstrapWidth = 860, bootstrapHeight = 320, banner = true)
		val bounds = SplashPanelGeometry.bounds(1920, 1080, transition)
		assertEquals(listOf(530f, 380f, 860f, 320f), bounds.toList())
	}

	@Test
	fun `a banner larger than the window is the window`() {
		// Otherwise the panel hangs off both edges and the text anchored to its top is off-screen.
		val transition = SplashTransitionConfiguration(bootstrapWidth = 860, bootstrapHeight = 320, banner = true)
		val bounds = SplashPanelGeometry.bounds(640, 200, transition)
		assertEquals(listOf(0f, 0f, 640f, 200f), bounds.toList())
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
