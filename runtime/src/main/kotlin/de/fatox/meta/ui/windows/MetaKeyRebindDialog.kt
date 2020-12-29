package de.fatox.meta.ui.windows

import com.badlogic.gdx.InputAdapter
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.components.MetaLabel

class MetaKeyRebindDialog : MetaDialog("Rebind Key", true) {
	val metaInput: MetaInputProcessor by lazyInject()

	init {
		contentTable.add(MetaLabel("Press the key you want to bind now.", 14)).center()
	}

	override fun show() {
		super.show()
		metaInput.exclusiveProcessor = RebindProcessor(this)
	}

	class RebindProcessor(private val metaKeyRebindDialog: MetaKeyRebindDialog) : InputAdapter() {
		override fun keyDown(keycode: Int): Boolean {
			metaKeyRebindDialog.close()
			return super.keyDown(keycode)
		}
	}
}
