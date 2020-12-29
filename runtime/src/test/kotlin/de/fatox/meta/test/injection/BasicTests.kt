package de.fatox.meta.test.injection

import de.fatox.meta.injection.Named
import org.junit.Assert

class BasicTests {
    class NamedTestModule {
        @Provides
        @Named("someName")
        @Singleton
        fun someString(): String {
            return "yeah"
        }
    }

    @Before
    fun prepare() {
        Meta.addModule(NamedTestModule())
    }

    class NamedTestSample {
        @Inject
        @Named("someName")
        var s: String? = null

        init {
            Meta.inject(this)
        }
    }

    @Test
    fun testNamed() {
        val namedTestSample = NamedTestSample()
        Assert.assertEquals("yeah", namedTestSample.s)
    }
}