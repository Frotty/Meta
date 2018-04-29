package de.fatox.meta.util

import java.util.regex.Pattern

object StringUtil {
    val SPACE = " "
    val EMPTY = ""
    val LF = "\n"
    val CR = "\r"
    private val VALID_FOLDER_REGEX = Pattern.compile("[-_.A-Za-z0-9, ]+")

    fun isValidFolderName(cs: CharSequence): Boolean {
        return VALID_FOLDER_REGEX.matcher(cs).matches()
    }

    fun isEmpty(cs: CharSequence?): Boolean {
        return cs == null || cs.length == 0
    }

    fun isBlank(cs: CharSequence?): Boolean {
        if (cs == null || cs.isEmpty()) {
            return true
        }
        for (i in 0 until cs.length) {
            if (!Character.isWhitespace(cs[i])) {
                return false
            }
        }
        return true
    }

    private fun regionMatches(cs: CharSequence, ignoreCase: Boolean, thisStart: Int,
                              substring: CharSequence, start: Int, length: Int): Boolean {
        if (cs is String && substring is String) {
            return cs.regionMatches(thisStart, substring, start, length, ignoreCase = ignoreCase)
        }
        var index1 = thisStart
        var index2 = start
        var tmpLen = length

        while (tmpLen-- > 0) {
            val c1 = cs[index1++]
            val c2 = substring[index2++]

            if (c1 == c2) {
                continue
            }

            if (!ignoreCase) {
                return false
            }

            // The same check as in String.regionMatches():
            if (Character.toUpperCase(c1) != Character.toUpperCase(c2) && Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                return false
            }
        }

        return true
    }

    fun truncate(s: String, chars: Int): String {
        return if (s.length > chars) {
            s.substring(0, chars - 1) + ".."
        } else s
    }
}
