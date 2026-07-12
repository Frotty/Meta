package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * Implemented by Meta widgets that cache a `BitmapFont` (or a style holding one) fetched from the
 * [de.fatox.meta.api.graphics.FontProvider]. When the UI scale changes the provider re-rasterizes its fonts at the
 * new physical resolution; [refreshFont] must then re-fetch the font and rebuild any cached
 * `BitmapFontCache`/styles so the widget stops referencing the old (about to be disposed) font.
 *
 * [MetaUIRenderer] walks the stage after every `uiScale` change and calls this on every implementing actor;
 * `MetaWindow` does the same for its own subtree when a hidden (off-stage, cached) window is re-shown.
 */
interface FontRefreshable {
	/** Re-fetches this widget's font(s) from the font provider and rebuilds cached glyph caches/styles. */
	fun refreshFont()
}

/**
 * Recursively calls [FontRefreshable.refreshFont] on this actor and every descendant, invalidating layouts along the
 * way so text re-measures with the new font metrics. This is only used on rare events (UI-scale change, re-showing a
 * cached window), so the recursion/iteration cost is acceptable.
 */
fun Actor.refreshFontsRecursively() {
	if (this is FontRefreshable) refreshFont()
	if (this is Group) {
		val children = children
		for (i in 0 until children.size) children.get(i).refreshFontsRecursively()
	}
	if (this is Layout) invalidateHierarchy()
}

/**
 * Tracks the [FontProvider.fontGeneration] a widget's cached fonts were fetched for. The renderer's on-scale-change
 * refresh walk only reaches actors that are on the stage at that moment; a widget living detached (inactive tab
 * content, a pooled list row, ...) keeps referencing fonts that are disposed right after the walk and would draw as
 * black squares once re-attached. Font-caching widgets own one tracker, call [refreshIfStale] from `setStage` on
 * attach to self-heal, and [markFresh] from [FontRefreshable.refreshFont] to record the re-fetch.
 */
class FontGenerationTracker {
	private val fontProvider: FontProvider by lazyInject()
	private var generation: Int = fontProvider.fontGeneration

	/** Records that the owning widget just (re)fetched its fonts; call from [FontRefreshable.refreshFont]. */
	fun markFresh() {
		generation = fontProvider.fontGeneration
	}

	/** Re-fetches the widget's fonts if the provider rebuilt them while the widget was off the stage. */
	fun refreshIfStale(widget: FontRefreshable) {
		if (generation != fontProvider.fontGeneration) widget.refreshFont()
	}
}
