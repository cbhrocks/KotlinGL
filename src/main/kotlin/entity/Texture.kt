package org.kotlingl.entity

import org.joml.Vector2f
import org.joml.Vector2fc
import org.kotlingl.shapes.GLResource
import org.lwjgl.opengl.ARBFramebufferObject.glGenerateMipmap
import org.lwjgl.opengl.GL11.GL_LINEAR
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL11.GL_REPEAT
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL11.glTexParameteri
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.math.floor

enum class WrapMode(val glEnum: Int) {
    REPEAT(GL_REPEAT),
    CLAMP(GL_CLAMP_TO_EDGE),
    MIRROR(GL_MIRRORED_REPEAT)
}

class Texture(
    //private val image: BufferedImage,
    val imageData: ByteBuffer,
    val width: Int,
    val height: Int,
    var wrapU: WrapMode = WrapMode.REPEAT,
    var wrapV: WrapMode = WrapMode.REPEAT,
    val uvIndex: Int = 0,
    val path: String = ""
): GLResource() {
    var glTextureId: Int = -1
        private set

    override fun initGL() {
        uploadToGPU()
        markInitialized()
    }

    private fun uploadToGPU() {
        if (glTextureId != -1) return // already uploaded

        glTextureId = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, glTextureId)

        // Set wrap modes
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapU.glEnum)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapV.glEnum)

        // Set filtering
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        // Upload pixel data to GPU
        glTexImage2D(
            GL_TEXTURE_2D, 0, GL_RGBA,
            width, height, 0,
            GL_RGBA, GL_UNSIGNED_BYTE, imageData
        )

        glGenerateMipmap(GL_TEXTURE_2D)

        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun sample(uv: Vector2fc): ColorRGB {
        val wrappedUV = getWrappedUV(uv)
        // Clamp or wrap UVs between 0 and 1

        val x = (wrappedUV.x() * width).toInt().coerceIn(0, width - 1)
        val y = ((1-wrappedUV.y()) * height).toInt().coerceIn(0, height - 1)
        val index = (y * width + x) * 4

        val r = (imageData.get(index).toInt() and 0xFF)
        val g = (imageData.get(index + 1).toInt() and 0xFF)
        val b = (imageData.get(index + 2).toInt() and 0xFF)
        val a = (imageData.get(index + 3).toInt() and 0xFF)

        return ColorRGB(r,g,b,a)
    }

    fun getWrappedUV(uv: Vector2fc): Vector2f {
        val u = applyWrap(uv.x(), wrapU)
        val v = applyWrap(uv.y(), wrapV)
        return Vector2f(u, v)
    }

    private fun applyWrap(value: Float, mode: WrapMode): Float = when (mode) {
        WrapMode.REPEAT -> value - floor(value)
        WrapMode.CLAMP -> value.coerceIn(0f, 1f)
        WrapMode.MIRROR -> {
            val floored = floor(value)
            val mirrored = if (floored % 2 == 0f) value - floored else 1f - (value - floored)
            mirrored.coerceIn(0f, 1f)
        }
    }

    companion object {
        val defaultWhite: Texture by lazy { createDefaultWhiteTexture() }
        val defaultNormal: Texture by lazy { createDefaultNormalTexture() }

        private fun createDefaultWhiteTexture(): Texture {
            val data = ByteBuffer.allocateDirect(4)
                .put(byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()))
            data.flip()
            val texture = Texture(
                data,
                1,
                1
            )
            texture.initGL()
            return texture
        }

        private fun createDefaultNormalTexture(): Texture {
            val data = ByteBuffer.allocateDirect(4)
                .put(byteArrayOf(128.toByte(), 128.toByte(), 255.toByte(), 255.toByte()))
            data.flip()
            val texture = Texture(
                data,
                1,
                1
            )
            texture.initGL()
            return texture
        }
    }
}
