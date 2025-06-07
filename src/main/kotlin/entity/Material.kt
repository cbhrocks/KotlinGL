package org.kotlingl.entity

import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3f
import org.lwjgl.assimp.AIColor4D
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIString
import org.lwjgl.assimp.Assimp
import org.lwjgl.assimp.Assimp.aiGetMaterialTexture
import org.lwjgl.assimp.Assimp.aiTextureType_DIFFUSE
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.file.Paths


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
    val uvScale: Vector2f = Vector2f(1f, 1f),
    val wrapMode: WrapMode = WrapMode.REPEAT
) {

    fun getWrappedUV(uvCoords: Vector2fc): Vector2f {
        return when (wrapMode) {
            WrapMode.REPEAT -> Vector2f(
                (uvCoords.x() % 1f + 1f) % 1f,
                (uvCoords.y() % 1f + 1f) % 1f
            );
            WrapMode.MIRROR -> {
                val flooredU = kotlin.math.floor(uvCoords.x())
                val flooredV = kotlin.math.floor(uvCoords.y())
                val fracU = uvCoords.x() - flooredU
                val fracV = uvCoords.y() - flooredV
                val u = if (flooredU.toInt() % 2 == 0) fracU else 1f - fracU
                val v = if (flooredV.toInt() % 2 == 0) fracV else 1f - fracV
                Vector2f(u,v)
            }
            WrapMode.CLAMP -> Vector2f(
                uvCoords.x().coerceIn(0f, 1f),
                uvCoords.y().coerceIn(0f, 1f),
            )
        }
    }
}
