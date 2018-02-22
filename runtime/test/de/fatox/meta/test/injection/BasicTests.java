package de.fatox.meta.test.injection;

import de.fatox.meta.Meta;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BasicTests {

    public static class NamedTestModule {

        @Provides
        @Named("someName")
        @Singleton
        public String someString() {
            return "yeah";
        }
    }

    @Before
    public void prepare() {
        Meta.addModule(new NamedTestModule());
    }

    public static class NamedTestSample {
        @Inject
        @Named("someName")
        public String s;

        public NamedTestSample() {
            Meta.inject(this);
        }
    }

    @Test
    public void testNamed() {
        NamedTestSample namedTestSample = new NamedTestSample();
        Assert.assertEquals("yeah", namedTestSample.s);
    }
}
