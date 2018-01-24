package de.fatox.meta.graphics.model;

import com.badlogic.gdx.Gdx;
import org.junit.Test;

public class MDXConverterTest {
    @Test
    public void testStuff() {
        MDXConverter.INSTANCE.convert(Gdx.files.internal("models/tcBox.mdx"));
    }
}