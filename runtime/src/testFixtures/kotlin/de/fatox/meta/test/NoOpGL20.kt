package de.fatox.meta.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

/**
 * A [GL20] that discards every call, implemented as a real class rather than a [java.lang.reflect.Proxy].
 *
 * [HeadlessGL20]'s proxy is the right trade for ordinary tests - 162 no-op overrides are a lot of source to express
 * "return zero". But a proxy cannot be used to measure allocation, because it *is* an allocation: every call through
 * `Proxy` boxes its primitive arguments into a fresh `Object[]`. Measured against Meta's own UI that came to roughly
 * 39 bytes per GL call, so one drawn frame appeared to allocate ~27 KB that Meta never allocated.
 *
 * This exists for exactly one job: let [AllocationProbe] measure a draw path without the instrument dominating the
 * reading. The bodies are generated from the interface and every one of them allocates nothing.
 *
 * Counting still works - it shares [GlCallRecorder] with the proxy - so a test may take both counters at once.
 * Prefer [HeadlessGL20] everywhere else: this class must be regenerated when libGDX changes `GL20`, and that cost is
 * only worth paying where it buys a real measurement.
 */
@Suppress("TooManyFunctions", "LargeClass")
object NoOpGL20 : GL20 {
	private var handle = 0
	private var previousGl: GL20? = null
	private var previousGl20: GL20? = null
	private var installed = false

	private fun nextHandle(): Int = ++handle

	/** Allocation-free tally hook: a single boolean test when nothing is recording. */
	private fun record(name: String) = GlCallRecorder.observe(name)

	/** libGDX reads compile and link status out of the buffer, so success is written rather than returned. */
	private fun reportSuccess(out: java.nio.IntBuffer) {
		if (out.remaining() > 0) out.put(out.position(), GL20.GL_TRUE)
	}

	/**
	 * Points [Gdx.gl] and [Gdx.gl20] at this stub, remembering what was there. Idempotent.
	 *
	 * Install it *after* [MetaHeadlessUi.install], which installs the proxy; this then takes over for the part of the
	 * test that measures.
	 */
	fun install() {
		if (installed) return
		previousGl = Gdx.gl
		previousGl20 = Gdx.gl20
		Gdx.gl = this
		Gdx.gl20 = this
		installed = true
	}

	/** Puts back whatever held the GL globals before [install]. Not optional - they are process-wide. */
	fun uninstall() {
		if (!installed) return
		Gdx.gl = previousGl
		Gdx.gl20 = previousGl20
		previousGl = null
		previousGl20 = null
		installed = false
	}

	override fun glActiveTexture(p0: Int): Unit { record("glActiveTexture") }
	override fun glBindTexture(p0: Int, p1: Int): Unit { record("glBindTexture") }
	override fun glBlendFunc(p0: Int, p1: Int): Unit { record("glBlendFunc") }
	override fun glClear(p0: Int): Unit { record("glClear") }
	override fun glClearColor(p0: Float, p1: Float, p2: Float, p3: Float): Unit { record("glClearColor") }
	override fun glClearDepthf(p0: Float): Unit { record("glClearDepthf") }
	override fun glClearStencil(p0: Int): Unit { record("glClearStencil") }
	override fun glColorMask(p0: Boolean, p1: Boolean, p2: Boolean, p3: Boolean): Unit { record("glColorMask") }
	override fun glCompressedTexImage2D(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: java.nio.Buffer): Unit { record("glCompressedTexImage2D") }
	override fun glCompressedTexSubImage2D(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: java.nio.Buffer): Unit { record("glCompressedTexSubImage2D") }
	override fun glCopyTexImage2D(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int): Unit { record("glCopyTexImage2D") }
	override fun glCopyTexSubImage2D(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int): Unit { record("glCopyTexSubImage2D") }
	override fun glCullFace(p0: Int): Unit { record("glCullFace") }
	override fun glDeleteTextures(p0: Int, p1: java.nio.IntBuffer): Unit { record("glDeleteTextures") }
	override fun glDeleteTexture(p0: Int): Unit { record("glDeleteTexture") }
	override fun glDepthFunc(p0: Int): Unit { record("glDepthFunc") }
	override fun glDepthMask(p0: Boolean): Unit { record("glDepthMask") }
	override fun glDepthRangef(p0: Float, p1: Float): Unit { record("glDepthRangef") }
	override fun glDisable(p0: Int): Unit { record("glDisable") }
	override fun glDrawArrays(p0: Int, p1: Int, p2: Int): Unit { record("glDrawArrays") }
	override fun glDrawElements(p0: Int, p1: Int, p2: Int, p3: java.nio.Buffer): Unit { record("glDrawElements") }
	override fun glEnable(p0: Int): Unit { record("glEnable") }
	override fun glFinish(): Unit { record("glFinish") }
	override fun glFlush(): Unit { record("glFlush") }
	override fun glFrontFace(p0: Int): Unit { record("glFrontFace") }
	override fun glGenTextures(p0: Int, p1: java.nio.IntBuffer): Unit { record("glGenTextures") }
	override fun glGenTexture(): Int { record("glGenTexture"); return nextHandle() }
	override fun glGetError(): Int { record("glGetError"); return 0 }
	override fun glGetIntegerv(p0: Int, p1: java.nio.IntBuffer): Unit { record("glGetIntegerv") }
	override fun glGetString(p0: Int): String { record("glGetString"); return "" }
	override fun glHint(p0: Int, p1: Int): Unit { record("glHint") }
	override fun glLineWidth(p0: Float): Unit { record("glLineWidth") }
	override fun glPixelStorei(p0: Int, p1: Int): Unit { record("glPixelStorei") }
	override fun glPolygonOffset(p0: Float, p1: Float): Unit { record("glPolygonOffset") }
	override fun glReadPixels(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: java.nio.Buffer): Unit { record("glReadPixels") }
	override fun glScissor(p0: Int, p1: Int, p2: Int, p3: Int): Unit { record("glScissor") }
	override fun glStencilFunc(p0: Int, p1: Int, p2: Int): Unit { record("glStencilFunc") }
	override fun glStencilMask(p0: Int): Unit { record("glStencilMask") }
	override fun glStencilOp(p0: Int, p1: Int, p2: Int): Unit { record("glStencilOp") }
	override fun glTexImage2D(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: java.nio.Buffer): Unit { record("glTexImage2D") }
	override fun glTexParameterf(p0: Int, p1: Int, p2: Float): Unit { record("glTexParameterf") }
	override fun glTexSubImage2D(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: java.nio.Buffer): Unit { record("glTexSubImage2D") }
	override fun glViewport(p0: Int, p1: Int, p2: Int, p3: Int): Unit { record("glViewport") }
	override fun glAttachShader(p0: Int, p1: Int): Unit { record("glAttachShader") }
	override fun glBindAttribLocation(p0: Int, p1: Int, p2: String): Unit { record("glBindAttribLocation") }
	override fun glBindBuffer(p0: Int, p1: Int): Unit { record("glBindBuffer") }
	override fun glBindFramebuffer(p0: Int, p1: Int): Unit { record("glBindFramebuffer") }
	override fun glBindRenderbuffer(p0: Int, p1: Int): Unit { record("glBindRenderbuffer") }
	override fun glBlendColor(p0: Float, p1: Float, p2: Float, p3: Float): Unit { record("glBlendColor") }
	override fun glBlendEquation(p0: Int): Unit { record("glBlendEquation") }
	override fun glBlendEquationSeparate(p0: Int, p1: Int): Unit { record("glBlendEquationSeparate") }
	override fun glBlendFuncSeparate(p0: Int, p1: Int, p2: Int, p3: Int): Unit { record("glBlendFuncSeparate") }
	override fun glBufferData(p0: Int, p1: Int, p2: java.nio.Buffer, p3: Int): Unit { record("glBufferData") }
	override fun glBufferSubData(p0: Int, p1: Int, p2: Int, p3: java.nio.Buffer): Unit { record("glBufferSubData") }
	override fun glCheckFramebufferStatus(p0: Int): Int { record("glCheckFramebufferStatus"); return 0 }
	override fun glCompileShader(p0: Int): Unit { record("glCompileShader") }
	override fun glCreateProgram(): Int { record("glCreateProgram"); return nextHandle() }
	override fun glCreateShader(p0: Int): Int { record("glCreateShader"); return nextHandle() }
	override fun glDeleteBuffer(p0: Int): Unit { record("glDeleteBuffer") }
	override fun glDeleteBuffers(p0: Int, p1: java.nio.IntBuffer): Unit { record("glDeleteBuffers") }
	override fun glDeleteFramebuffer(p0: Int): Unit { record("glDeleteFramebuffer") }
	override fun glDeleteFramebuffers(p0: Int, p1: java.nio.IntBuffer): Unit { record("glDeleteFramebuffers") }
	override fun glDeleteProgram(p0: Int): Unit { record("glDeleteProgram") }
	override fun glDeleteRenderbuffer(p0: Int): Unit { record("glDeleteRenderbuffer") }
	override fun glDeleteRenderbuffers(p0: Int, p1: java.nio.IntBuffer): Unit { record("glDeleteRenderbuffers") }
	override fun glDeleteShader(p0: Int): Unit { record("glDeleteShader") }
	override fun glDetachShader(p0: Int, p1: Int): Unit { record("glDetachShader") }
	override fun glDisableVertexAttribArray(p0: Int): Unit { record("glDisableVertexAttribArray") }
	override fun glDrawElements(p0: Int, p1: Int, p2: Int, p3: Int): Unit { record("glDrawElements") }
	override fun glEnableVertexAttribArray(p0: Int): Unit { record("glEnableVertexAttribArray") }
	override fun glFramebufferRenderbuffer(p0: Int, p1: Int, p2: Int, p3: Int): Unit { record("glFramebufferRenderbuffer") }
	override fun glFramebufferTexture2D(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int): Unit { record("glFramebufferTexture2D") }
	override fun glGenBuffer(): Int { record("glGenBuffer"); return nextHandle() }
	override fun glGenBuffers(p0: Int, p1: java.nio.IntBuffer): Unit { record("glGenBuffers") }
	override fun glGenerateMipmap(p0: Int): Unit { record("glGenerateMipmap") }
	override fun glGenFramebuffer(): Int { record("glGenFramebuffer"); return nextHandle() }
	override fun glGenFramebuffers(p0: Int, p1: java.nio.IntBuffer): Unit { record("glGenFramebuffers") }
	override fun glGenRenderbuffer(): Int { record("glGenRenderbuffer"); return nextHandle() }
	override fun glGenRenderbuffers(p0: Int, p1: java.nio.IntBuffer): Unit { record("glGenRenderbuffers") }
	override fun glGetActiveAttrib(p0: Int, p1: Int, p2: java.nio.IntBuffer, p3: java.nio.IntBuffer): String { record("glGetActiveAttrib"); return "" }
	override fun glGetActiveUniform(p0: Int, p1: Int, p2: java.nio.IntBuffer, p3: java.nio.IntBuffer): String { record("glGetActiveUniform"); return "" }
	override fun glGetAttachedShaders(p0: Int, p1: Int, p2: java.nio.Buffer, p3: java.nio.IntBuffer): Unit { record("glGetAttachedShaders") }
	override fun glGetAttribLocation(p0: Int, p1: String): Int { record("glGetAttribLocation"); return 0 }
	override fun glGetBooleanv(p0: Int, p1: java.nio.Buffer): Unit { record("glGetBooleanv") }
	override fun glGetBufferParameteriv(p0: Int, p1: Int, p2: java.nio.IntBuffer): Unit { record("glGetBufferParameteriv") }
	override fun glGetFloatv(p0: Int, p1: java.nio.FloatBuffer): Unit { record("glGetFloatv") }
	override fun glGetFramebufferAttachmentParameteriv(p0: Int, p1: Int, p2: Int, p3: java.nio.IntBuffer): Unit { record("glGetFramebufferAttachmentParameteriv") }
	override fun glGetProgramiv(p0: Int, p1: Int, p2: java.nio.IntBuffer) {
		record("glGetProgramiv")
		reportSuccess(p2)
	}
	override fun glGetProgramInfoLog(p0: Int): String { record("glGetProgramInfoLog"); return "" }
	override fun glGetRenderbufferParameteriv(p0: Int, p1: Int, p2: java.nio.IntBuffer): Unit { record("glGetRenderbufferParameteriv") }
	override fun glGetShaderiv(p0: Int, p1: Int, p2: java.nio.IntBuffer) {
		record("glGetShaderiv")
		reportSuccess(p2)
	}
	override fun glGetShaderInfoLog(p0: Int): String { record("glGetShaderInfoLog"); return "" }
	override fun glGetShaderPrecisionFormat(p0: Int, p1: Int, p2: java.nio.IntBuffer, p3: java.nio.IntBuffer): Unit { record("glGetShaderPrecisionFormat") }
	override fun glGetTexParameterfv(p0: Int, p1: Int, p2: java.nio.FloatBuffer): Unit { record("glGetTexParameterfv") }
	override fun glGetTexParameteriv(p0: Int, p1: Int, p2: java.nio.IntBuffer): Unit { record("glGetTexParameteriv") }
	override fun glGetUniformfv(p0: Int, p1: Int, p2: java.nio.FloatBuffer): Unit { record("glGetUniformfv") }
	override fun glGetUniformiv(p0: Int, p1: Int, p2: java.nio.IntBuffer): Unit { record("glGetUniformiv") }
	override fun glGetUniformLocation(p0: Int, p1: String): Int { record("glGetUniformLocation"); return 0 }
	override fun glGetVertexAttribfv(p0: Int, p1: Int, p2: java.nio.FloatBuffer): Unit { record("glGetVertexAttribfv") }
	override fun glGetVertexAttribiv(p0: Int, p1: Int, p2: java.nio.IntBuffer): Unit { record("glGetVertexAttribiv") }
	override fun glGetVertexAttribPointerv(p0: Int, p1: Int, p2: java.nio.Buffer): Unit { record("glGetVertexAttribPointerv") }
	override fun glIsBuffer(p0: Int): Boolean { record("glIsBuffer"); return false }
	override fun glIsEnabled(p0: Int): Boolean { record("glIsEnabled"); return false }
	override fun glIsFramebuffer(p0: Int): Boolean { record("glIsFramebuffer"); return false }
	override fun glIsProgram(p0: Int): Boolean { record("glIsProgram"); return false }
	override fun glIsRenderbuffer(p0: Int): Boolean { record("glIsRenderbuffer"); return false }
	override fun glIsShader(p0: Int): Boolean { record("glIsShader"); return false }
	override fun glIsTexture(p0: Int): Boolean { record("glIsTexture"); return false }
	override fun glLinkProgram(p0: Int): Unit { record("glLinkProgram") }
	override fun glReleaseShaderCompiler(): Unit { record("glReleaseShaderCompiler") }
	override fun glRenderbufferStorage(p0: Int, p1: Int, p2: Int, p3: Int): Unit { record("glRenderbufferStorage") }
	override fun glSampleCoverage(p0: Float, p1: Boolean): Unit { record("glSampleCoverage") }
	override fun glShaderBinary(p0: Int, p1: java.nio.IntBuffer, p2: Int, p3: java.nio.Buffer, p4: Int): Unit { record("glShaderBinary") }
	override fun glShaderSource(p0: Int, p1: String): Unit { record("glShaderSource") }
	override fun glStencilFuncSeparate(p0: Int, p1: Int, p2: Int, p3: Int): Unit { record("glStencilFuncSeparate") }
	override fun glStencilMaskSeparate(p0: Int, p1: Int): Unit { record("glStencilMaskSeparate") }
	override fun glStencilOpSeparate(p0: Int, p1: Int, p2: Int, p3: Int): Unit { record("glStencilOpSeparate") }
	override fun glTexParameterfv(p0: Int, p1: Int, p2: java.nio.FloatBuffer): Unit { record("glTexParameterfv") }
	override fun glTexParameteri(p0: Int, p1: Int, p2: Int): Unit { record("glTexParameteri") }
	override fun glTexParameteriv(p0: Int, p1: Int, p2: java.nio.IntBuffer): Unit { record("glTexParameteriv") }
	override fun glUniform1f(p0: Int, p1: Float): Unit { record("glUniform1f") }
	override fun glUniform1fv(p0: Int, p1: Int, p2: java.nio.FloatBuffer): Unit { record("glUniform1fv") }
	override fun glUniform1fv(p0: Int, p1: Int, p2: FloatArray, p3: Int): Unit { record("glUniform1fv") }
	override fun glUniform1i(p0: Int, p1: Int): Unit { record("glUniform1i") }
	override fun glUniform1iv(p0: Int, p1: Int, p2: java.nio.IntBuffer): Unit { record("glUniform1iv") }
	override fun glUniform1iv(p0: Int, p1: Int, p2: IntArray, p3: Int): Unit { record("glUniform1iv") }
	override fun glUniform2f(p0: Int, p1: Float, p2: Float): Unit { record("glUniform2f") }
	override fun glUniform2fv(p0: Int, p1: Int, p2: java.nio.FloatBuffer): Unit { record("glUniform2fv") }
	override fun glUniform2fv(p0: Int, p1: Int, p2: FloatArray, p3: Int): Unit { record("glUniform2fv") }
	override fun glUniform2i(p0: Int, p1: Int, p2: Int): Unit { record("glUniform2i") }
	override fun glUniform2iv(p0: Int, p1: Int, p2: java.nio.IntBuffer): Unit { record("glUniform2iv") }
	override fun glUniform2iv(p0: Int, p1: Int, p2: IntArray, p3: Int): Unit { record("glUniform2iv") }
	override fun glUniform3f(p0: Int, p1: Float, p2: Float, p3: Float): Unit { record("glUniform3f") }
	override fun glUniform3fv(p0: Int, p1: Int, p2: java.nio.FloatBuffer): Unit { record("glUniform3fv") }
	override fun glUniform3fv(p0: Int, p1: Int, p2: FloatArray, p3: Int): Unit { record("glUniform3fv") }
	override fun glUniform3i(p0: Int, p1: Int, p2: Int, p3: Int): Unit { record("glUniform3i") }
	override fun glUniform3iv(p0: Int, p1: Int, p2: java.nio.IntBuffer): Unit { record("glUniform3iv") }
	override fun glUniform3iv(p0: Int, p1: Int, p2: IntArray, p3: Int): Unit { record("glUniform3iv") }
	override fun glUniform4f(p0: Int, p1: Float, p2: Float, p3: Float, p4: Float): Unit { record("glUniform4f") }
	override fun glUniform4fv(p0: Int, p1: Int, p2: java.nio.FloatBuffer): Unit { record("glUniform4fv") }
	override fun glUniform4fv(p0: Int, p1: Int, p2: FloatArray, p3: Int): Unit { record("glUniform4fv") }
	override fun glUniform4i(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int): Unit { record("glUniform4i") }
	override fun glUniform4iv(p0: Int, p1: Int, p2: java.nio.IntBuffer): Unit { record("glUniform4iv") }
	override fun glUniform4iv(p0: Int, p1: Int, p2: IntArray, p3: Int): Unit { record("glUniform4iv") }
	override fun glUniformMatrix2fv(p0: Int, p1: Int, p2: Boolean, p3: java.nio.FloatBuffer): Unit { record("glUniformMatrix2fv") }
	override fun glUniformMatrix2fv(p0: Int, p1: Int, p2: Boolean, p3: FloatArray, p4: Int): Unit { record("glUniformMatrix2fv") }
	override fun glUniformMatrix3fv(p0: Int, p1: Int, p2: Boolean, p3: java.nio.FloatBuffer): Unit { record("glUniformMatrix3fv") }
	override fun glUniformMatrix3fv(p0: Int, p1: Int, p2: Boolean, p3: FloatArray, p4: Int): Unit { record("glUniformMatrix3fv") }
	override fun glUniformMatrix4fv(p0: Int, p1: Int, p2: Boolean, p3: java.nio.FloatBuffer): Unit { record("glUniformMatrix4fv") }
	override fun glUniformMatrix4fv(p0: Int, p1: Int, p2: Boolean, p3: FloatArray, p4: Int): Unit { record("glUniformMatrix4fv") }
	override fun glUseProgram(p0: Int): Unit { record("glUseProgram") }
	override fun glValidateProgram(p0: Int): Unit { record("glValidateProgram") }
	override fun glVertexAttrib1f(p0: Int, p1: Float): Unit { record("glVertexAttrib1f") }
	override fun glVertexAttrib1fv(p0: Int, p1: java.nio.FloatBuffer): Unit { record("glVertexAttrib1fv") }
	override fun glVertexAttrib2f(p0: Int, p1: Float, p2: Float): Unit { record("glVertexAttrib2f") }
	override fun glVertexAttrib2fv(p0: Int, p1: java.nio.FloatBuffer): Unit { record("glVertexAttrib2fv") }
	override fun glVertexAttrib3f(p0: Int, p1: Float, p2: Float, p3: Float): Unit { record("glVertexAttrib3f") }
	override fun glVertexAttrib3fv(p0: Int, p1: java.nio.FloatBuffer): Unit { record("glVertexAttrib3fv") }
	override fun glVertexAttrib4f(p0: Int, p1: Float, p2: Float, p3: Float, p4: Float): Unit { record("glVertexAttrib4f") }
	override fun glVertexAttrib4fv(p0: Int, p1: java.nio.FloatBuffer): Unit { record("glVertexAttrib4fv") }
	override fun glVertexAttribPointer(p0: Int, p1: Int, p2: Int, p3: Boolean, p4: Int, p5: java.nio.Buffer): Unit { record("glVertexAttribPointer") }
	override fun glVertexAttribPointer(p0: Int, p1: Int, p2: Int, p3: Boolean, p4: Int, p5: Int): Unit { record("glVertexAttribPointer") }
}
