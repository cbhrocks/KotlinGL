package org.kotlingl.renderer

import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.glBindFramebuffer

class Compositor(val width: Int, val height: Int) {
    val backgroundFB = Framebuffer.create(width, height)
    val worldFB = Framebuffer.create(width, height)
    val uiFB = Framebuffer.create(width, height)

    fun composeToScreen() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }
}