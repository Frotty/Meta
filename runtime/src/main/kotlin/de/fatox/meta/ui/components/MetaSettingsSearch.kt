package de.fatox.meta.ui.components

import com.badlogic.gdx.utils.Align
import de.fatox.meta.reactive.Disposable
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaType

/** An indexed setting. [open] dismisses results, mounts the destination tab and returns its target row. */
class MetaSettingsSearchEntry(
	val title: String,
	val section: String,
	val keywords: List<String> = emptyList(),
	val open: () -> MetaHighlight?,
)

/**
 * Search results across settings tabs. The caller can place this view beside or in place of existing tab content.
 * Selecting a result runs its navigation callback synchronously, then scrolls to and highlights the returned row.
 */
class MetaSettingsSearch @JvmOverloads constructor(emptyMessage: String = "No matching settings") : MetaStack() {
	private val results = MetaActionList<MetaSettingsSearchEntry> { entry, _ ->
		MetaActionRow(title = entry.title, subtitle = entry.section, density = MetaActionRowDensity.COMFORTABLE)
			.onActivate {
				if (entry in matches) entry.open()?.reveal()
			}.apply { setSearchHighlight(query) }
	}
	private val emptyState = MetaLabel(emptyMessage, MetaType.BODY, MetaColor.TEXT_MUTED).apply {
		setAlignment(Align.center)
		setWrap(true)
		isVisible = false
	}
	private var entries = emptyList<MetaSettingsSearchEntry>()
	private var matches = emptyList<MetaSettingsSearchEntry>()

	var query: String = ""
		set(value) {
			val trimmed = value.trim()
			if (field == trimmed) return
			field = trimmed
			refresh()
		}

	init {
		addItem(results)
		addItem(emptyState)
	}

	/** Replaces the index, for example when the settings or their translations change. */
	fun submit(entries: List<MetaSettingsSearchEntry>) {
		this.entries = entries.toList()
		refresh()
	}

	fun bindQuery(scope: ReactiveScope, query: () -> String): Disposable =
		scope.effect("MetaSettingsSearch.query") { this.query = query() }

	private fun refresh() {
		val searching = query.isNotEmpty()
		matches = if (searching) entries.filter { entry ->
			entry.title.contains(query, ignoreCase = true) ||
				entry.section.contains(query, ignoreCase = true) ||
				entry.keywords.any { it.contains(query, ignoreCase = true) }
		} else emptyList()
		results.submit(matches)
		results.scrollPane.setScrollY(0f)
		results.scrollPane.updateVisualScroll()
		emptyState.isVisible = searching && matches.isEmpty()
	}
}
