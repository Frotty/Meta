package de.fatox.meta.input

import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.task.MetaTask

class MetaShortcut(callback: MetaTask?, vararg keycodes: Int) {
	private val metaInput: MetaInputProcessor by lazyInject()
	private val missingKeys: Int = keycodes.size

	init {
// TODO use new metaInput
//        for(int code : keycodes) {
//            metaInput.registerKeyListener(code, new KeyListener() {
//                @Override
//                void onEvent() {
//                }
//                @Override
//                public void onDown() {
//                    missingKeys--;
//                    if(missingKeys <= 0) {
//                        callback.execute();
//                    }
//                }
//                @Override
//                public void onUp() {
//                    missingKeys++;
//                }
//            });
//        }
	}
}