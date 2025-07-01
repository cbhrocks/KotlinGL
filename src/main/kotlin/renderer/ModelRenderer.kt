package org.kotlingl.renderer

import org.kotlingl.Camera
import org.kotlingl.Scene
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import java.nio.ByteBuffer

class ModelRenderer(var width: Int, var height: Int): Renderer {

    fun traceRays(scene: Scene) {
        val camera = scene.cameraManager.activeCamera
        val rays = camera.generateRays()

        // create pixel buffer
        val pixels = ByteBuffer.allocateDirect(rays.size * 3)
        // put colors for each ray in pixel buffer
        rays.forEachIndexed { index, ray ->
            val color = scene.traceRay(ray)

            pixels.put(color.r.toByte())
            pixels.put(color.g.toByte())
            pixels.put(color.b.toByte())
            //pixels.put((color.x * 255f).toInt().coerceIn(0, 255).toByte())
            //pixels.put((color.y * 255f).toInt().coerceIn(0, 255).toByte())
            //pixels.put((color.z * 255f).toInt().coerceIn(0, 255).toByte())
        }
        // upload pixel buffer to texture
        pixels.rewind()

        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT) // clear the framebuffer

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, camera.textureId!!)
        GL11.glTexSubImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            0,
            0,
            width,
            height,
            GL11.GL_RGB,
            GL11.GL_UNSIGNED_BYTE,
            pixels
        )

        // Draw texture on full-screen quad
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(-1f, -1f)
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f( 1f, -1f)
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f( 1f,  1f)
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(-1f,  1f)
        GL11.glEnd()
    }

    /**
     * 1. Bind framebuffer with color texture attached
     * 2. Clear buffers
     * 3. Set camera matrices
     * 4. Draw all models in scene
     * 5. Unbind framebuffer
     */
    //private fun renderSceneToTexture(scene: Scene, camera: Camera): Int {
    //    //return framebuffer.colorTextureID
    //}

    //private fun drawFullscreen(textureID: Int, shader: ShaderProgram? = defaultPostShader) {

    //}

    //private fun loop() {
        //// Set the clear color

        //traceRays(this.scene.activeCamera)

        //// Poll for window events. The key callback above will only be
        //// invoked during this call.
        //GLFW.glfwPollEvents()
    //}

    override fun render(
        scene: Scene,
        camera: Camera,
        target: Framebuffer
    ) {

    }
}

