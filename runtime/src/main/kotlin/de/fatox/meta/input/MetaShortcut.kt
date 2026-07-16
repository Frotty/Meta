package de.fatox.meta.input

import de.fatox.meta.task.MetaTask

/** Legacy placeholder retained only for source and binary compatibility; it never registered a shortcut. */
@Deprecated("Register a KeyListener with MetaInputProcessor")
class MetaShortcut(
	@Suppress("UNUSED_PARAMETER") callback: MetaTask?,
	@Suppress("UNUSED_PARAMETER") vararg keycodes: Int,
)
