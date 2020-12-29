package de.fatox.meta.input

import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.injection.Inject
import de.fatox.meta.task.MetaTask

class MetaShortcut(callback: MetaTask?, vararg keycodes: Int) {
    @Inject
    private val metaInput: MetaInput? = null
    private val missingKeys: Int

    init {
        inject(this)
        missingKeys = keycodes.size
        // TODO use new metainput
        //        for(int code : keycodes) {
        //            metaInput.registerKeyListener(code, new KeyListener() {
        //                @Override
        //                void onEvent() {
        //
        //                }
        //
        //                @Override
        //                public void onDown() {
        //                    missingKeys--;
        //                    if(missingKeys <= 0) {
        //                        callback.execute();
        //                    }
        //                }
        //
        //                @Override
        //                public void onUp() {
        //                    missingKeys++;
        //                }
        //            });
        //        }
    }
}