package de.fatox.meta.error;

import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.injection.Inject;

public class MetaErrors {
    @Inject
    private LanguageBundle languageBundle;

    private Array<MetaError> errors = new Array<>();

    public MetaErrors() {
        Meta.inject(this);
    }

    public void add(MetaError metaError) {
        errors.add(metaError);
    }

    public boolean hasErrors() {
        return errors.size > 0;
    }

    public String getLabelText() {
        if(hasErrors()) {
            if(errors.size>1) {
                return languageBundle.format("error_found", errors.size);
            } else {
                return errors.get(0).getName();
            }
        }
        return "";
    }
}
