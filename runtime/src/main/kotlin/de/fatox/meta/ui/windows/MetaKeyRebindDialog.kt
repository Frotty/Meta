package de.fatox.meta.ui.windows

import com.badlogic.gdx.InputAdapter
import de.fatox.meta.injection.Inject
import de.fatox.meta.input.MetaInput
import de.fatox.meta.ui.components.MetaLabel

class MetaKeyRebindDialog : MetaDialog("Rebind Key", true) {
    @Inject
    lateinit var metaInput: MetaInput

    init {
        contentTable.add(MetaLabel("Press the key you want to bind now.", 14)).center()
    }

    override fun show() {
        super.show()
        metaInput.setExclusiveProcessor(RebindProcessor(this))
    }

    class RebindProcessor(val metaKeyRebindDialog: MetaKeyRebindDialog) : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            metaKeyRebindDialog.close()
            return super.keyDown(keycode)
        }
    }
}
