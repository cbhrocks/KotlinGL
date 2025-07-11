package org.kotlingl.entity

import ShaderProgram
import org.joml.Vector2f
import org.joml.Vector2fc
import org.kotlingl.shapes.GLResource
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.GL_TEXTURE1
import org.lwjgl.opengl.GL13.GL_TEXTURE2
import org.lwjgl.opengl.GL13.glActiveTexture

data class Material(
    val diffuseTexture: Texture? = null,
    val normalTexture: Texture? = null,
    val specularTexture: Texture? = null,
    var baseColor: ColorRGB = ColorRGB(150, 150, 150),
    var reflect: Float = 0f,
    val shininess: Float = 1f,
    val uvScale: Vector2fc = Vector2f(1f, 1f),
    val name: String? = null
): GLResource() {

    override fun initGL(){
        diffuseTexture?.initGL()
        specularTexture?.initGL()
        normalTexture?.initGL()
        markInitialized()
    }

    fun bind(shader: ShaderProgram) {
        // Bind diffuse texture
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, diffuseTexture?.glTextureId ?: Texture.defaultWhite.glTextureId)
        shader.setUniform("material.diffuse", 0)

        // Bind normal texture
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, normalTexture?.glTextureId ?: Texture.defaultNormal.glTextureId)
        shader.setUniform("material.normalMap", 1)

        // Bind specular texture
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, specularTexture?.glTextureId ?: Texture.defaultWhite.glTextureId)
        shader.setUniform("material.specular", 2)

        // Set non-texture material properties
        shader.setUniform("material.baseColor", baseColor.toVector3f())
        shader.setUniform("material.reflect", reflect)
        shader.setUniform("material.shininess", shininess)
        shader.setUniform("material.uvScale", uvScale)
    }

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