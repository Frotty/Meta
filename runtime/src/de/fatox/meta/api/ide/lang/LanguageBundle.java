package de.fatox.meta.api.ide.lang;


public interface LanguageBundle {
    void loadBundle(AvailableLanguages lang);

    String get(String key);
}
