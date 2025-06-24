package org.kotlingl.lighting

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.minus
import org.joml.plus
import org.joml.times
import org.joml.unaryMinus
import org.kotlingl.entity.Intersection
import org.kotlingl.Scene
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.toColor
import org.kotlingl.math.*
import org.kotlingl.shapes.Ray
import kotlin.math.pow

class Shader private constructor(
    private val stages: List<ShadingStage>
){
    fun shade(hit: Intersection, scene: Scene, bounce: Int): ColorRGB {
        return stages.fold(Vector3f(0f, 0f, 0f)) { acc, stage ->
            acc + stage.shade(hit, scene, bounce)
        }.toColor()
    }

    class Builder( ) {
        private val stages = mutableListOf<ShadingStage>()

        fun addStage(stage: ShadingStage): Builder {
            stages += stage
            return this
        }

        fun build(): Shader = Shader(stages)
    }
}

interface ShadingStage {
    fun shade(hit: Intersection, scene: Scene, bounce: Int): Vector3f
}

class AmbientStage(
    val intensity: Float
) : ShadingStage {
    override fun shade(hit: Intersection, scene: Scene, bounce: Int): Vector3f {
        return (hit.material.diffuseTexture?.sample(hit.uv ?: Vector2f(0f, 0f))
            ?: hit.material.baseColor).toVector3f().mul(this.intensity)
    }
}

class DiffuseStage : ShadingStage {
    override fun shade(hit: Intersection, scene: Scene, bounce: Int): Vector3f {
        return scene.lights.fold(Vector3f()) { acc, light ->
            val lightDir = light.getDirection(hit.point)

            //backfacing rejection
            val nDotL = hit.normal.dot(lightDir)
            // if the surface facing the light
            val facesLight = nDotL > 0
            if (!facesLight) {
                return@fold acc
            }

            if (light.contributesToShadows) {
                val shadowRay = Ray(hit.point + hit.normal * EPSILON, lightDir)
                val inShadow = scene.intersect(shadowRay)?.let {
                    it.t < light.getDistance(hit.point)
                } ?: false
                if (inShadow){
                    return@fold acc
                }
            }

            val intensity = light.computeIntensity(hit, lightDir)

            val color = hit.material.diffuseTexture?.sample(hit.uv ?: Vector2f(0f, 0f))
                ?: hit.material.baseColor

            acc + color.toVector3f() * intensity
        }
    }
}

class SpecularStage : ShadingStage {
    override fun shade(hit: Intersection, scene: Scene, bounce: Int): Vector3f {
        // Simple Phong specular model (placeholder)
        return scene.lights.fold (Vector3f(0f, 0f, 0f)) { acc, light ->
            val lightDir = light.getDirection(hit.point)

            if (light.contributesToShadows) {
                val shadowRay = Ray(hit.point + hit.normal * EPSILON, lightDir)
                val inShadow = scene.intersect(shadowRay)?.let {
                    it.t < light.getDistance(hit.point)
                } ?: false
                if (inShadow){
                    return@fold acc
                }
            }

            val viewDir = (scene.activeCamera.position - hit.point).normalize()
            val reflectDir = reflect(-lightDir, hit.normal)
            val spec = maxOf(viewDir.dot(reflectDir), 0f).pow(hit.material.shininess)
            acc + light.computeIntensity(hit, lightDir) * spec // white specular
        }
    }

    private fun reflect(dir: Vector3f, normal: Vector3fc): Vector3f {
        return dir - normal * 2f * dir.dot(normal)
    }
}
