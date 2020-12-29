package de.fatox.meta.lang;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import de.fatox.meta.Meta;
import de.fatox.meta.api.lang.AvailableLanguages;
import de.fatox.meta.api.lang.LanguageBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class MetaLanguageBundle implements LanguageBundle {
	private static final Logger log = LoggerFactory.getLogger(MetaLanguageBundle.class);
    private I18NBundle currentBundle;

    private AvailableLanguages currentLanguage;

    public MetaLanguageBundle() {
        Meta.inject(this);
        loadBundle(AvailableLanguages.EN);
    }

    @Override
    public void loadBundle(AvailableLanguages lang) {
        FileHandle baseFileHandle = Gdx.files.internal("lang/MetagineBundle");
        Locale locale = new Locale(lang.toString().toLowerCase(), lang.toString(), "");
        try {
            currentBundle = I18NBundle.createBundle(baseFileHandle, locale);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }
        log.debug("Loaded: " + lang);
        log.debug("Current Locale: " + currentBundle.getLocale());
    }

    /**
     * Returns the S
     *
     * @param key
     */
    @Override
    public String get(String key) {
        return currentBundle.get(key);
    }

    @Override
    public String format(String key, Object... args) {
        return currentBundle.format(key, args);
    }
}
