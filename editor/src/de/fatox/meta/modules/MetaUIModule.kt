package de.fatox.meta.modules

import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.injection.Provides
import de.fatox.meta.injection.Singleton
import de.fatox.meta.lang.MetaLanguageBundle
import de.fatox.meta.ui.MetaEditorUI

/**
 * Created by Frotty on 07.06.2016.
 */
class MetaUIModule {
	init {
	    MetaInject.global {
			singleton("meta", "gameName")
			singleton { MetaData() }
			singleton { MetaEditorUI() }
			singleton<LanguageBundle> { MetaLanguageBundle() }
		}
	}

	@Provides
	@Singleton
	fun metaData(): MetaData = inject()

	@Provides
	@Singleton
	fun metaEditorUI(): MetaEditorUI = inject()
}