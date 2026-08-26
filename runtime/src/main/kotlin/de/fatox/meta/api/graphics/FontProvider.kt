package de.fatox.meta.api.graphics

import com.badlogic.gdx.graphics.g2d.BitmapFont

interface FontProvider {
	fun getFont(size: Int, type: FontType): BitmapFont

	fun write(x: Float, y: Float, text: String, size: Int, type: FontType)

	/**
	 * Monotonic counter, incremented every time the cached fonts are rebuilt (e.g. after a UI-scale change).
	 * Widgets that keep a font while off-stage (cached windows) compare against this to detect a stale font and
	 * re-fetch on re-show. Default (for custom providers that never rebuild): constant 0.
	 */
	val fontGeneration: Int get() = 0

	/**
	 * Disposes fonts that were orphaned by a rebuild (see [fontGeneration]). Must only be called after every live
	 * widget has re-fetched its font (e.g. after the UI renderer's stage walk), on the GL thread. Default: no-op.
	 */
	/**
	 * Opens the configured faces without touching the GL thread, so the files are parsed before anything draws.
	 *
	 * Called by [de.fatox.meta.api.SplashScreen] on its preparation worker. Implementations must not create
	 * textures here — reading and parsing the face is the part that can move off the GL thread; rasterizing and
	 * uploading is not. Optional: the default does nothing, and a provider that ignores it simply opens faces on
	 * first use as before.
	 */
	fun prepareFaces() {}

	fun disposeOrphanedFonts() {}

	/** Disposes all generated fonts and font generators. Call once at application shutdown. Default: no-op. */
	fun dispose() {}
}
