package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.utils.Layout

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
