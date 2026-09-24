package de.fatox.meta.ui

import de.fatox.meta.api.lang.Localization
import de.fatox.meta.reactive.Disposable
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTextButton

/** Keeps a label translated when [Localization.currentLanguage] changes. */
fun MetaLabel.bindLocalizedText(localization: Localization, key: String): Disposable =
	bindText { localization[key] }

/** Keeps a button translated when [Localization.currentLanguage] changes. */
fun MetaTextButton.bindLocalizedText(localization: Localization, key: String): Disposable =
	bindText { localization[key] }

fun ReactiveScope.bindLocalizedText(
	label: MetaLabel,
	localization: Localization,
	key: String,
): Disposable = register(label.bindLocalizedText(localization, key))

fun ReactiveScope.bindLocalizedText(
	button: MetaTextButton,
	localization: Localization,
	key: String,
): Disposable = register(button.bindLocalizedText(localization, key))
