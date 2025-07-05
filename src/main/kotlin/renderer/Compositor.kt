package org.kotlingl.renderer

import ShaderProgram
import org.kotlingl.shapes.ScreenQuad
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL30.*

class Compositor(var renderWidth: Int, var renderHeight: Int, var viewportWidth: Int, var viewportHeight: Int) {
    private val screenQuad = ScreenQuad()
    private val quadShader: ShaderProgram

    val renderTargets = mutableMapOf(
        "background" to Framebuffer.create(renderWidth, renderHeight),
        "world" to Framebuffer.create(renderWidth, renderHeight),
        "ui" to Framebuffer.create(renderWidth, renderHeight),
    )

    fun initGL() {
        screenQuad.initGL()
    }

    init {
        val vertexSource = ShaderProgram.loadShaderSource("/shaders/fullscreen_quad.vert")
        val fragmentSource = ShaderProgram.loadShaderSource("/shaders/fullscreen_quad.frag")
        quadShader = ShaderProgram(vertexSource, fragmentSource)
    }

    fun composeToScreen() {
        // Bind default framebuffer (screen)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)

        glViewport(0, 0, viewportWidth, viewportHeight)
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Bind quad shader and texture
        quadShader.use()
        glBindVertexArray(screenQuad.vaoId)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, renderTargets.getValue("background").textureId)
        quadShader.setUniform("screenTexture", 0)

        // Draw fullscreen quad
        glDrawArrays(GL_TRIANGLES, 0, 6)
    }

    fun getTarget(renderTarget: String): Framebuffer {
        return renderTargets.getValue(renderTarget)
    }

    fun clearBuffers() {
        for (target in renderTargets) {
            target.value.bind()
            GL11.glClearColor(50f, 0f, 0f, 1f)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    fun resize(width: Int, height: Int) {
        this.renderWidth = width
        this.renderHeight = height
        renderTargets.mapValues {
            it.key to it.value.resize(width, height)
        }
    }
}