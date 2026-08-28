package de.fatox.meta.api

import com.badlogic.gdx.Graphics

interface MonitorHandler {
	fun monitorIndex(displayMode: Graphics.DisplayMode): Int

	/**
	 * What the operating system's display scaling is set to, as a factor - 1.5 for Windows at 150%.
	 *
	 * Needed because the two platforms express HiDPI in opposite ways. macOS hands a Retina window a framebuffer
	 * twice the window size, so the back-buffer ratio reveals the scaling and the application draws more pixels
	 * without doing anything. Windows does not: a DPI-aware process gets a framebuffer exactly the size it asked
	 * for, and the window is simply physically smaller on a dense panel. The back-buffer ratio is therefore always
	 * 1.0 there and says nothing at all.
	 *
	 * So this is the only signal on Windows that the user asked for a larger interface. Returns 1 where there is
	 * no scaling or no way to ask.
	 */
	val osContentScale: Float get() = 1f
}

object NoMonitorHandler : MonitorHandler {
	override fun monitorIndex(displayMode: Graphics.DisplayMode): Int = 0
}