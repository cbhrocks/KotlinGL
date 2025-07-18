package org.kotlingl.renderer

import org.kotlingl.Camera
import org.kotlingl.RayTraceContext
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

    fun rayTraceScene(scene: Scene, camera: Camera, resX: Int, resY: Int): ByteBuffer {
        val rays = camera.generateRays(resX, resY)

        // create pixel buffer
        val pixels = ByteBuffer.allocateDirect(rays.size * 4)
        // put colors for each ray in pixel buffer
        rays.forEachIndexed { index, ray ->
            val color = scene.traceRay(ray, RayTraceContext(
                scene, camera, setOf("background")
            ))

            pixels.put(color.r.toByte())
            pixels.put(color.g.toByte())
            pixels.put(color.b.toByte())
            pixels.put(color.a.toByte())
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
        val buffer = rayTraceScene(scene, camera, target.width, target.height)
        target.uploadBuffer(buffer)
    }
}