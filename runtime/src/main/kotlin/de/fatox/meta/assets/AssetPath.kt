package de.fatox.meta.assets

import java.util.Locale

/**
 * Portable lookup key for an asset name. The physical source path remains untouched; only lookup is normalized.
 * This makes archive and raw-file loading behave the same on case-sensitive and case-insensitive file systems.
 */
internal fun assetPathKey(path: String): String = path.replace('\\', '/').lowercase(Locale.ROOT)
