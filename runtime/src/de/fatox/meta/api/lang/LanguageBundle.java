package de.fatox.meta.api.lang;


public interface LanguageBundle {
    void loadBundle(AvailableLanguages lang);

    String get(String key);
}
