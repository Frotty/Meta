package de.fatox.meta.modules

import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.injection.Provides
import de.fatox.meta.injection.Singleton
import de.fatox.meta.ui.MetaEditorUI

/**
 * Created by Frotty on 07.06.2016.
 */
class MetaUIModule {
	init {
	    MetaInject.global {
			singleton { MetaData() }
			singleton { MetaEditorUI() }
		}
	}

	@Provides
	@Singleton
	fun metaData(): MetaData = inject()

	@Provides
	@Singleton
	fun metaEditorUI(): MetaEditorUI = inject()
}