package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import de.fatox.meta.reactive.Disposable
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.ui.MetaSpacing

/**
 * A column of searchable components, suitable as the content of a [MetaScrollPane].
 * Matches a trimmed, case-insensitive substring against each entry's labels and optional keywords.
 * Hidden entries stay mounted and collapse out of layout. A blank query restores all entries.
 */
class MetaSearchList @JvmOverloads constructor(gap: Float = MetaSpacing.XS) : MetaStack() {
	private class Entry(val row: MetaHighlight, val labels: Array<out MetaLabel>, val keywords: List<String>)

	private val rows = MetaFlexBox(direction = MetaFlexDirection.COLUMN, mainGap = gap, align = MetaFlexAlign.STRETCH)
	private val entries = mutableListOf<Entry>()

	var query: String = ""
		set(value) {
			val trimmed = value.trim()
			if (field == trimmed) return
			field = trimmed
			refreshMatches()
		}

	init {
		touchable = Touchable.childrenOnly
		rows.touchable = Touchable.childrenOnly
		addItem(rows)
	}

	/** Adds a component and the labels within it that should be searched and highlighted. */
	fun addEntry(content: Actor, vararg labels: MetaLabel, keywords: List<String> = emptyList()) {
		require(content.parent == null) { "Search entry must have no parent" }
		val entry = Entry(MetaHighlight(content), labels.copyOf(), keywords.toList())
		entries.add(entry)
		rows.addItem(entry.row, minWidth = 0f, shrink = 0f)
		updateMatch(entry)
	}

	/** Binds a search field or other reactive source using the caller's screen or window scope. */
	fun bindQuery(scope: ReactiveScope, query: () -> String): Disposable =
		scope.effect("MetaSearchList.query") { this.query = query() }

	/** Re-evaluates the current query after a caller changes searchable label text. */
	fun refreshMatches() {
		for (index in entries.indices) updateMatch(entries[index])
	}

	private fun updateMatch(entry: Entry) {
		val searching = query.isNotEmpty()
		var labelMatch = false
		for (index in entry.labels.indices) {
			val label = entry.labels[index]
			label.setHighlight(query)
			if (searching && label.text.contains(query, ignoreCase = true)) labelMatch = true
		}
		val keywordMatch = searching && !labelMatch && entry.keywords.any { it.contains(query, ignoreCase = true) }
		val visible = !searching || labelMatch || keywordMatch
		entry.row.highlighted = keywordMatch
		val losingFocus = !visible && entry.row.isVisible
		rows.setItemVisible(entry.row, visible)
		if (losingFocus) {
			val stage = entry.row.stage ?: return
			stage.unfocus(entry.row)
			cancelChildTouchFocus(stage, entry.row)
		}
	}

	private fun cancelChildTouchFocus(stage: Stage, group: Group) {
		val children = group.children
		val count = children.size
		val snapshot = children.begin()
		try {
			for (index in 0 until count) {
				val child = snapshot[index]
				stage.cancelTouchFocus(child)
				if (child is Group) cancelChildTouchFocus(stage, child)
			}
		} finally {
			children.end()
		}
	}
}
