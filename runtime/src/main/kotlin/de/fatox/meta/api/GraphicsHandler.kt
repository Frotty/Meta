package de.fatox.meta.api

import de.fatox.meta.graphics.buffer.MultisampleFBO

interface GraphicsHandler {
	fun createTexture(fbo: MultisampleFBO, attachmentSpec: MultisampleFBO.FrameBufferTextureAttachmentSpec): Int
	fun build(fbo: MultisampleFBO)
}

object NoGraphicsHandler : GraphicsHandler {
	override fun createTexture(
		fbo: MultisampleFBO,
		attachmentSpec: MultisampleFBO.FrameBufferTextureAttachmentSpec
	): Int = -1

	override fun build(fbo: MultisampleFBO): Unit = Unit
}