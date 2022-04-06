package de.fatox.meta.modules

import com.badlogic.gdx.Gdx
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.lang.MetaLanguageBundle
import de.fatox.meta.ui.MetaEditorUI

/**
 * Created by Frotty on 07.06.2016.
 */
object MetaUIModule {
	init {
		MetaInject.global {
			singleton("meta", "gameName")
			singleton { MetaData() }
			singleton { MetaEditorUI() }
			singleton<LanguageBundle> { MetaLanguageBundle(Gdx.files.internal("lang/MetagineBundle")) }
		}
	}
}