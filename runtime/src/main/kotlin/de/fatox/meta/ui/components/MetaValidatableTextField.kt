package de.fatox.meta.ui.components

import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.ui.MetaType

/** Compatibility name for [MetaInputField]. New code should use [MetaInputField] directly. */
@Deprecated(
	message = "Use MetaInputField",
	replaceWith = ReplaceWith("MetaInputField(text, size, fontProvider)"),
)
class MetaValidatableTextField @JvmOverloads constructor(
	text: String = "",
	size: Int = MetaType.BODY,
	fontProvider: FontProvider = inject(),
) : MetaInputField(text, size, fontProvider)
