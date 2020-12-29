package de.fatox.meta.test

import com.badlogic.gdx.backends.headless.HeadlessApplication

abstract class MetaTest {
    @Before
    fun prepare() {
        val config = HeadlessApplicationConfiguration()
        HeadlessApplication(TestApp(), config)
        Meta.inject(this)
    }
}