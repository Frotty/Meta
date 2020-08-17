@file:JvmName("StringUtil")

package de.fatox.meta.util

private val VALID_FOLDER_REGEX = Regex("[-_.A-Za-z0-9, ]+")

fun String.truncate(chars: Int): String =
	if (length > chars) substring(0, chars - 1) + ".." else this

fun CharSequence.isValidFolderName(): Boolean =
	VALID_FOLDER_REGEX.matches(this)