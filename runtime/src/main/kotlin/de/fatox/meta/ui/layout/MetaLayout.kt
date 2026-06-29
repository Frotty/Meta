package de.fatox.meta.ui.layout

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.Layout

/**
 * Static layout-sanity helpers so agents (and tests) can verify a scene2d/VisUI layout actually fits, instead of
 * eyeballing the running game. It is pure geometry over the scene graph - no GL/Gdx needed - so it runs in plain
 * headless unit tests: build your `Table`, give it a size (or `pack()` it), then assert.
 *
 * It finds two classes of problem:
 *  - [Kind.OVERFLOW]  - a child sticks out past its parent's box (content spilling outside the container).
 *  - [Kind.CLIPPED]   - a [Layout] actor is smaller than its own preferred size, so its content is squished/cut
 *                       (e.g. a label too narrow for its text).
 *
 * Content inside a [ScrollPane] is intentionally allowed to exceed the viewport, so its subtree is skipped.
 *
 * Note: real text measurement needs a rasterized TTF font (GL). In headless tests use fixed-size stand-in widgets
 * (override `getPrefWidth`/`getPrefHeight`); the checks themselves are font-agnostic.
 */
object MetaLayout {

	enum class Kind { OVERFLOW, CLIPPED }

	data class Problem(val kind: Kind, val actor: Actor, val path: String, val detail: String) {
		override fun toString(): String = "[$kind] $path: $detail"
	}

	/**
	 * Lays out [root] (if it is a [Layout]) and returns every layout problem found in its subtree. An empty list
	 * means the layout fits. [tolerance] is the sub-pixel slack (in px) tolerated before something counts as a
	 * problem. Set [checkClipping] to false to only look for out-of-bounds children.
	 */
	fun problems(root: Actor, tolerance: Float = 0.5f, checkClipping: Boolean = true): List<Problem> {
		(root as? Layout)?.validate()
		val problems = ArrayList<Problem>()
		inspect(root, root.javaClass.simpleName, tolerance, checkClipping, problems)
		return problems
	}

	/** Throws [AssertionError] with a readable report if [problems] finds anything. No-op when the layout fits. */
	fun assertValid(root: Actor, tolerance: Float = 0.5f, checkClipping: Boolean = true) {
		val problems = problems(root, tolerance, checkClipping)
		if (problems.isNotEmpty()) {
			throw AssertionError(
				problems.joinToString(
					prefix = "Layout has ${problems.size} problem(s):\n  ",
					separator = "\n  ",
				),
			)
		}
	}

	private fun inspect(
		actor: Actor,
		path: String,
		tolerance: Float,
		checkClipping: Boolean,
		out: MutableList<Problem>,
	) {
		if (checkClipping && actor is Layout) {
			val prefW = actor.prefWidth
			val prefH = actor.prefHeight
			// prefWidth/Height of 0 means "no preference" (e.g. wrapped label) - not a clip.
			if (prefW > actor.width + tolerance) {
				out += Problem(Kind.CLIPPED, actor, path, "needs width ${prefW.r()} but has ${actor.width.r()}")
			}
			if (prefH > actor.height + tolerance) {
				out += Problem(Kind.CLIPPED, actor, path, "needs height ${prefH.r()} but has ${actor.height.r()}")
			}
		}

		if (actor !is Group) return
		// ScrollPane content is meant to exceed the viewport; don't descend into it.
		if (actor is ScrollPane) return

		for (child in actor.children) {
			val childPath = "$path>${child.javaClass.simpleName.ifEmpty { "<anon>" }}"
			if (child.x < -tolerance) {
				out += Problem(Kind.OVERFLOW, child, childPath, "x=${child.x.r()} < 0 (left edge)")
			}
			if (child.y < -tolerance) {
				out += Problem(Kind.OVERFLOW, child, childPath, "y=${child.y.r()} < 0 (bottom edge)")
			}
			if (child.x + child.width > actor.width + tolerance) {
				out += Problem(
					Kind.OVERFLOW, child, childPath,
					"right=${(child.x + child.width).r()} > parent width ${actor.width.r()}",
				)
			}
			if (child.y + child.height > actor.height + tolerance) {
				out += Problem(
					Kind.OVERFLOW, child, childPath,
					"top=${(child.y + child.height).r()} > parent height ${actor.height.r()}",
				)
			}
			inspect(child, childPath, tolerance, checkClipping, out)
		}
	}

	/** Rounds to one decimal for readable messages. */
	private fun Float.r(): String {
		val tenths = Math.round(this * 10)
		return "${tenths / 10}.${Math.abs(tenths % 10)}"
	}
}
