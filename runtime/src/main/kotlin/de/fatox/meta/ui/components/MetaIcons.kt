package de.fatox.meta.ui.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap

object MetaIcons {
	const val INDEX_PATH: String = "ui/icons/remixicon.tsv"

	private val icons: ObjectMap<String, IconInfo> by lazy { loadIndex() }

	data class IconInfo(
		val name: String,
		val codepoint: Int,
		val category: String,
	)

	fun normalize(name: String): String {
		return if (name.startsWith("ri-")) name else "ri-$name"
	}

	fun exists(name: String): Boolean {
		return icons.containsKey(normalize(name))
	}

	fun info(name: String): IconInfo? {
		return icons[normalize(name)]
	}

	fun glyph(name: String): String {
		val iconName = normalize(name)
		val info = icons[iconName] ?: throw GdxRuntimeException(unknownIconMessage(iconName))
		return String(Character.toChars(info.codepoint))
	}

	fun names(): Array<String> {
		val result = Array<String>(icons.size)
		val keys = icons.keys()
		while (keys.hasNext()) result.add(keys.next())
		result.sort()
		return result
	}

	fun entries(): Array<IconInfo> {
		val result = Array<IconInfo>(icons.size)
		val values = icons.values()
		while (values.hasNext()) result.add(values.next())
		result.sort { a, b -> a.name.compareTo(b.name) }
		return result
	}

	fun search(query: String, limit: Int = 25): Array<IconInfo> {
		val needle = normalizeQuery(query)
		val result = Array<IconInfo>()
		if (needle.isEmpty() || limit <= 0) return result

		val values = icons.values()
		while (values.hasNext()) {
			val info = values.next()
			if (info.name.lowercase().contains(needle) || info.category.lowercase().contains(needle)) {
				result.add(info)
			}
		}
		result.sort { a, b -> a.name.compareTo(b.name) }
		if (result.size <= limit) return result

		val limited = Array<IconInfo>(limit)
		for (i in 0 until limit) limited.add(result[i])
		return limited
	}

	private fun loadIndex(): ObjectMap<String, IconInfo> {
		val result = ObjectMap<String, IconInfo>()
		val file = Gdx.files.internal(INDEX_PATH)
		if (!file.exists()) {
			throw GdxRuntimeException("Remix Icon index not found: $INDEX_PATH")
		}

		val lines = file.readString("UTF-8").lineSequence()
		for (line in lines) {
			if (line.isEmpty() || line[0] == '#') continue
			val columns = line.split('\t')
			if (columns.size < 2) continue

			val name = columns[0]
			val codepoint = columns[1].toInt(16)
			val category = if (columns.size > 2) columns[2] else ""
			result.put(name, IconInfo(name, codepoint, category))
		}
		return result
	}

	private fun unknownIconMessage(iconName: String): String {
		val stem = iconName.removePrefix("ri-").substringBeforeLast('-', iconName.removePrefix("ri-"))
		val suggestions = search(stem, limit = 5)
		if (suggestions.isEmpty) {
			return "Unknown Remix icon '$iconName'. Search $INDEX_PATH for supported names."
		}

		val builder = StringBuilder("Unknown Remix icon '")
		builder.append(iconName)
		builder.append("'. Did you mean ")
		for (i in 0 until suggestions.size) {
			if (i > 0) builder.append(", ")
			builder.append(suggestions[i].name)
		}
		builder.append("? Search ")
		builder.append(INDEX_PATH)
		builder.append(" for the full catalog.")
		return builder.toString()
	}

	private fun normalizeQuery(query: String): String {
		return query.removePrefix("ri-").lowercase()
	}
}
