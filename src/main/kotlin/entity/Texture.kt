package org.kotlingl.entity

import org.joml.Vector2f
import org.joml.Vector2fc
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.math.floor

enum class WrapMode {
    REPEAT,
    CLAMP,
    MIRROR
}

class Texture(
    //private val image: BufferedImage,
    val imageData: ByteBuffer,
    val width: Int,
    val height: Int,
    val wrapU: WrapMode,
    val wrapV: WrapMode,
    val uvIndex: Int = 0,
    val path: String = ""
) {

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

        //fun fromImageResource(path: String): Texture {
        //    val stream = object {}.javaClass.getResourceAsStream(path)
        //        ?: throw IllegalArgumentException("Resource not found: $path")
        //    val bImage = ImageIO.read(stream) ?: throw IllegalArgumentException("Could not read image: $path")
        //    return Texture(
        //        bImage,
        //        WrapMode.REPEAT,
        //        WrapMode.REPEAT,
        //    )
        //}

        fun fromAssimp(aiTexture: Texture) {

        }
    }
}