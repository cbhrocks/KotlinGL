package org.kotlingl.entity

import org.kotlingl.math.Vector2


enum class WrapMode {
    REPEAT,
    CLAMP,
    MIRROR
}

data class Material(
    var color: ColorRGB = ColorRGB(150, 150, 150),
    var texture: Texture? = null,
    var reflect: Float = 0f,
    val shininess: Float = 1f,
    val uvScale: Vector2 = Vector2(1f, 1f),
    val wrapMode: WrapMode = WrapMode.REPEAT
) {

    fun getWrappedUV(uvCoords: Vector2): Vector2 {
        return when (wrapMode) {
            WrapMode.REPEAT -> Vector2(
                (uvCoords.x % 1f + 1f) % 1f,
                (uvCoords.y % 1f + 1f) % 1f
            );
            WrapMode.MIRROR -> {
                val flooredU = kotlin.math.floor(uvCoords.x)
                val flooredV = kotlin.math.floor(uvCoords.y)
                val fracU = uvCoords.x - flooredU
                val fracV = uvCoords.y - flooredV
                val u = if (flooredU.toInt() % 2 == 0) fracU else 1f - fracU
                val v = if (flooredV.toInt() % 2 == 0) fracV else 1f - fracV
                Vector2(u,v)
            }
            WrapMode.CLAMP -> Vector2(
                uvCoords.x.coerceIn(0f, 1f),
                uvCoords.y.coerceIn(0f, 1f),
            )
        }
    }
}