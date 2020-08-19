package de.fatox.meta.test;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import de.fatox.meta.Meta;
import org.junit.Before;

public abstract class MetaTest {

    @Before
    public void prepare() {
        final HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        new HeadlessApplication(new TestApp(), config);
        Meta.inject(this);
    }
}
