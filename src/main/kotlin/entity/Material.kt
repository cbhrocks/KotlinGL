package org.kotlingl.entity

import org.joml.Vector2f
import org.joml.Vector2fc

data class Material(
    val diffuseTexture: Texture? = null,
    val normalTexture: Texture? = null,
    val specularTexture: Texture? = null,
    var baseColor: ColorRGB = ColorRGB(150, 150, 150),
    var reflect: Float = 0f,
    val shininess: Float = 1f,
    val uvScale: Vector2fc = Vector2f(1f, 1f),
) {
    fun sampleDiffuse(uv: Vector2fc): ColorRGB {
        return diffuseTexture?.sample(uv) ?: baseColor
    }

    fun sampleNormal(uv: Vector2fc): ColorRGB {
        return normalTexture?.sample(uv) ?: ColorRGB(0, 0, 255)
    }

    fun sampleSpecular(uv: Vector2fc): ColorRGB {
        return specularTexture?.sample(uv) ?: ColorRGB.WHITE
    }
}