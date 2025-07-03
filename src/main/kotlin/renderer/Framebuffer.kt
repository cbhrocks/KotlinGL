package org.kotlingl.renderer

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL30.GL_RENDERBUFFER
import org.lwjgl.opengl.GL30.glBindFramebuffer
import org.lwjgl.opengl.GL30.glBindRenderbuffer
import org.lwjgl.opengl.GL30.glCheckFramebufferStatus
import org.lwjgl.opengl.GL30.glDeleteFramebuffers
import org.lwjgl.opengl.GL30.glDeleteRenderbuffers
import org.lwjgl.opengl.GL30.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL30.glFramebufferTexture2D
import org.lwjgl.opengl.GL30.glGenFramebuffers
import org.lwjgl.opengl.GL30.glGenRenderbuffers
import org.lwjgl.opengl.GL30.glRenderbufferStorage
import java.nio.ByteBuffer

data class Framebuffer (
    val id: Int,
    val textureId: Int,
    val width: Int,
    val height: Int,
    val depthBufferId: Int? = null // Optional for depth-only or depth+color FBOs
) {
    fun destroy() {
        glDeleteFramebuffers(id)
        glDeleteTextures(textureId)
        depthBufferId?.let { glDeleteRenderbuffers(it) }
    }

    fun resize(newWidth: Int, newHeight: Int): Framebuffer {
        destroy()
        return create(newWidth, newHeight)
    }

    fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, id)
        glViewport(0, 0, width, height)
    }

    fun bindTexture() {
        glBindTexture(GL_TEXTURE_2D, textureId)
    }

    fun uploadBuffer(buffer: ByteBuffer) {
        val bufferLength = buffer.capacity()/4
        val texturePixels = width * height
        require( bufferLength == texturePixels) {
            "buffer ($bufferLength) is not the same size as the texture target ($texturePixels)"
        }
        bindTexture()
        glTexSubImage2D(
            GL_TEXTURE_2D,
            0, 0, 0,
            width, height,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            buffer
        )
    }

    companion object {
        fun create(width: Int, height: Int): Framebuffer {
            val framebuffer = glGenFramebuffers()
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)

            val texture = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, texture)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0)

            val depthBuffer = glGenRenderbuffers()
            glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer)
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height)
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer)

            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw RuntimeException("Framebuffer is not complete")
            }

            glBindFramebuffer(GL_FRAMEBUFFER, 0)

            return Framebuffer(framebuffer, texture, width, height, depthBuffer)
        }
    }
}