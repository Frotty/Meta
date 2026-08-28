package de.fatox.meta

import com.badlogic.gdx.utils.Array
import de.fatox.meta.assets.MetaDataKey

/*
 * Named for the module rather than for what it holds, and that is the whole point.
 *
 * Kotlin names a file's class after the file: two `MetaDataKeys.kt` in package `de.fatox.meta`, one here and one in
 * `runtime`, both compiled to `de.fatox.meta.MetaDataKeysKt`. Each module built cleanly on its own, and on a
 * combined classpath the editor's came first and shadowed the runtime's - so `Meta.create` died with
 * NoSuchMethodError looking for `getAudioVideoDataKey()`, a method that existed and was simply unreachable.
 *
 * Keep top-level declarations in a shared package in files whose names cannot collide across modules.
 */

val lastProjectsKey = MetaDataKey<Array<String>>("lastProjects")
