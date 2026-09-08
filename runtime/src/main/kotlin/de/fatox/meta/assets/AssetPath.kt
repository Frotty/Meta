package de.fatox.meta.assets

import java.util.Locale

/**
 * The one lookup key for an asset name, shared by loose files, v1 entries and v2 archives.
 *
 * The physical source path is left untouched; only lookup is normalised, which makes archive and raw-file loading
 * behave the same on case-sensitive and case-insensitive file systems.
 *
 * It applies [normalisedPath] as well as case folding, so `./data//shared.bin` and `data/shared.bin` are one key.
 * That matters beyond tidiness: v2 name hashes are computed over this form, and while this function only folded
 * slashes and case the two disagreed - an indexed lookup missed where a packed one hit, so the same asset resolved
 * differently by spelling and an overlap between the two sources went unreported. One rule, one function, so they
 * cannot drift again.
 */
internal fun assetPathKey(path: String): String = normalisedPath(path).lowercase(Locale.ROOT)
