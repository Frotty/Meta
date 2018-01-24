package test;

import com.badlogic.gdx.Gdx;
import de.fatox.meta.graphics.model.MDXConverter;
import org.junit.Test;

public class MDXConverterTest {
    @Test
    public void testStuff() {
        MDXConverter.INSTANCE.convert(Gdx.files.internal("models/tcBox.mdx"));
    }
}