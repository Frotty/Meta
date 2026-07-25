package de.fatox.meta.modules

import com.badlogic.gdx.Gdx
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.lang.MetaLanguageBundle
import de.fatox.meta.ui.MetaEditorUI

object MetaUIModule {
	/** Forces this module's registration block to run before editor services are resolved. */
	internal fun initialize() = Unit

	init {
		MetaInject.global {
			singleton("meta", "gameName")
			singleton { MetaData() }
			singleton { MetaEditorUI() }
			singleton<LanguageBundle> { MetaLanguageBundle(Gdx.files.internal("lang/MetagineBundle")) }
		}
	}
}
