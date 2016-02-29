package de.fatox.meta.input;

import de.fatox.meta.Meta;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.task.MetaTask;


public class MetaShortcut {
    @Inject
    private MetaInput metaInput;

    private int missingKeys;

    public MetaShortcut(final MetaTask callback, int... keycodes) {
        Meta.inject(this);
        missingKeys = keycodes.length;
        for(int code : keycodes) {
            metaInput.registerKeyListener(code, new KeyListener() {
                @Override
                public void onDown() {
                    missingKeys--;
                    if(missingKeys <= 0) {
                        callback.execute();
                    }
                }

                @Override
                public void onUp() {
                    missingKeys++;
                }
            });
        }
    }

}
