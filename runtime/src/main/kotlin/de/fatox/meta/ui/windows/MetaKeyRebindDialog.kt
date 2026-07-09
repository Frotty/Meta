package de.fatox.meta.ui.windows

import com.badlogic.gdx.InputAdapter
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.components.MetaLabel

class MetaKeyRebindDialog : MetaDialog("Rebind Key", true) {
	val metaInput: MetaInputProcessor by lazyInject()

	private var rebindProcessor: RebindProcessor? = null

	init {
		contentTable.add(MetaLabel("Press the key you want to bind now.", 14)).center()
	}

	override fun show() {
		super.show()
		// Guard against double show: only ever hold ONE pushed grab, so onHidden can pop exactly what we pushed.
		if (rebindProcessor == null) {
			rebindProcessor = RebindProcessor(this).also { metaInput.pushExclusiveProcessor(it) }
		}
	}

	override fun onHidden() {
		// Critical: release the exclusive grab so input returns to the rest of the UI once this dialog is gone,
		// no matter how it closed. Pop by owner (not peek-and-pop-top) so a grab nested on top of ours doesn't
		// bury and leak our processor.
		rebindProcessor?.let { metaInput.popExclusiveProcessor(it) }
		rebindProcessor = null
	}

	class RebindProcessor(private val metaKeyRebindDialog: MetaKeyRebindDialog) : InputAdapter() {
		override fun keyDown(keycode: Int): Boolean {
			metaKeyRebindDialog.close()
			return super.keyDown(keycode)
		}
	}
}
