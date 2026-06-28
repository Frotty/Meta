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

	override fun onHidden() {
		// Critical: release the exclusive grab so input returns to the rest of the UI once this dialog is gone,
		// no matter how it closed. Leaving it set would freeze all other input behind this disposed dialog.
		if (metaInput.exclusiveProcessor is RebindProcessor) {
			metaInput.exclusiveProcessor = null
		}
	}

	class RebindProcessor(private val metaKeyRebindDialog: MetaKeyRebindDialog) : InputAdapter() {
		override fun keyDown(keycode: Int): Boolean {
			metaKeyRebindDialog.close()
			return super.keyDown(keycode)
		}
	}
}
