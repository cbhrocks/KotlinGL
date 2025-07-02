package org.kotlingl.renderer

import org.kotlingl.Camera
import org.kotlingl.Scene
import org.kotlingl.entity.Texture
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glTexSubImage2D
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.glBindFramebuffer
import java.nio.ByteBuffer

class RayTraceRenderer(): Renderer {

    fun rayTraceScene(scene: Scene, camera: Camera): ByteBuffer {
        val rays = camera.generateRays()

        // create pixel buffer
        val pixels = ByteBuffer.allocateDirect(rays.size * 3)
        // put colors for each ray in pixel buffer
        rays.forEachIndexed { index, ray ->
            val color = scene.traceRay("background", ray)

            pixels.put(color.r.toByte())
            pixels.put(color.g.toByte())
            pixels.put(color.b.toByte())
            //pixels.put((color.x * 255f).toInt().coerceIn(0, 255).toByte())
            //pixels.put((color.y * 255f).toInt().coerceIn(0, 255).toByte())
            //pixels.put((color.z * 255f).toInt().coerceIn(0, 255).toByte())
        }
        // upload pixel buffer to texture
        pixels.rewind()

        return pixels
    }

    override fun render(
        scene: Scene,
        camera: Camera,
        target: Framebuffer
    ) {
        val buffer = rayTraceScene(scene, camera)

        glBindFramebuffer(GL_FRAMEBUFFER, target.id)
        glBindTexture(GL_TEXTURE_2D, target.textureId)
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, target.width, target.height, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }
}