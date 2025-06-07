package org.kotlingl.entity

import org.joml.Vector2fc
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class Texture(private val image: BufferedImage) {

    fun sample(uv: Vector2fc): ColorRGB {
        // Clamp or wrap UVs between 0 and 1
        val u = (uv.x() % 1.0f + 1.0f) % 1.0f
        val v = (uv.y() % 1.0f + 1.0f) % 1.0f

        val x = (u * image.width).toInt().coerceIn(0, image.width - 1)
        val y = ((1-v) * image.height).toInt().coerceIn(0, image.height - 1)

        val rgb = image.getRGB(x, y)
        return ColorRGB.fromRGB(rgb) // Wraps Java Color or your own Color class
    }

    companion object {

        fun fromImageResource(path: String): Texture {
            val stream = object {}.javaClass.getResourceAsStream(path)
                ?: throw IllegalArgumentException("Resource not found: $path")
            val bImage = ImageIO.read(stream) ?: throw IllegalArgumentException("Could not read image: $path")
            return Texture(
                bImage
            )
        }
    }
}