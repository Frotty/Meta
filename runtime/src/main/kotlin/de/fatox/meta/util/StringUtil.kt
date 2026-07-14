@file:JvmName("StringUtil")

package de.fatox.meta.util

private val VALID_FOLDER_REGEX = Regex("[-_.A-Za-z0-9, ]+")

fun String.truncate(chars: Int): String {
	val limit = chars.coerceAtLeast(0)
	return if (length > limit) substring(0, (limit - 1).coerceAtLeast(0)) + ".." else this
}

fun CharSequence.isValidFolderName(): Boolean = VALID_FOLDER_REGEX.matches(this)
