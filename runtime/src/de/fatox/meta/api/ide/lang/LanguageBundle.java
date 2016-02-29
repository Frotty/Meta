package de.fatox.meta.api.ide.lang;

import de.fatox.meta.ide.lang.AvailableLanguages;

public interface LanguageBundle {
    void loadBundle(AvailableLanguages lang);

    String get(String key);
}
