package de.fatox.meta.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

/**
 * A GL20 that accepts everything and does nothing.
 *
 * Installing it is what turns the headless backend from "no graphics device" into "a graphics device that discards
 * every call". That distinction is the whole reason real widget trees can be built in a unit test: libGDX only needs
 * GL to *upload* a texture, not to measure one, so a `Texture` whose upload went nowhere still reports its size, a
 * FreeType face still rasterizes to a `Pixmap` on the CPU, and a [de.fatox.meta.ui.components.MetaLabel] built on
 * that font still measures its text correctly. Without a stub every one of those steps dies on `Gdx.gl` being null.
 *
 * Written as a [Proxy] rather than 350 hand-written no-op overrides. A test fixture is the one place where that
 * trade is obviously right: the alternative is a thousand lines that have to be revisited every time libGDX adds a
 * method, to express "return a zero".
 *
 * ### What it reports as succeeding, and why
 *
 * Object creation hands out real, distinct handles, and shader compile/link status reports success. That is what lets
 * `SpriteBatch` and therefore `Stage` be constructed, which in turn is what lets a *screen* be tested rather than a
 * reconstruction of one: Meta's screens own their stage, so a harness that cannot make one forces every consumer to
 * rebuild each layout inside its test — the duplication this fixture exists to avoid.
 *
 * The trade is explicit: **measurements are real, pixels are not.** Every draw call is discarded, so a test may lay
 * out, measure and validate a tree, and must not assert anything about what was rendered.
 */
object HeadlessGL20 {
	/** Real handles, so anything that keys a cache on an object id sees distinct objects. */
	private val nextHandle = AtomicInteger(1)

	/** The queries libGDX checks before deciding a shader or program is usable. */
	private val successQueries = setOf("glGetShaderiv", "glGetProgramiv")

	/** Calls that must hand back a usable name rather than zero. */
	private val handleFactories = setOf(
		"glGenTexture", "glGenBuffer", "glCreateShader", "glCreateProgram", "glGenFramebuffer",
		"glGenRenderbuffer",
	)

	private val stub: GL20 by lazy {
		Proxy.newProxyInstance(GL20::class.java.classLoader, arrayOf(GL20::class.java)) { _, method, args ->
			if (method.name in successQueries) reportSuccess(args)
			when (method.returnType) {
				Int::class.javaPrimitiveType -> if (method.name in handleFactories) nextHandle.getAndIncrement() else 0
				Long::class.javaPrimitiveType -> 0L
				Boolean::class.javaPrimitiveType -> false
				Float::class.javaPrimitiveType -> 0f
				String::class.java -> ""
				else -> null
			}
		} as GL20
	}

	/**
	 * Writes `GL_TRUE` into the out-parameter of a status query. libGDX reads compile and link status through an
	 * `IntBuffer` or an `int[]`, so returning a value is not enough — the answer goes in the argument.
	 */
	private fun reportSuccess(args: Array<Any?>?) {
		val out = args?.lastOrNull() ?: return
		when (out) {
			is java.nio.IntBuffer -> if (out.remaining() > 0) out.put(out.position(), GL20.GL_TRUE)
			is IntArray -> if (out.isNotEmpty()) out[0] = GL20.GL_TRUE
			else -> Unit
		}
	}

	/** Points [Gdx.gl] and [Gdx.gl20] at the stub. Idempotent. */
	fun install() {
		if (Gdx.gl === stub) return
		Gdx.gl = stub
		Gdx.gl20 = stub
	}
}
