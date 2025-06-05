package org.kotlingl.lighting

import org.joml.Vector3f
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

class DiffuseStage : ShadingStage {
    override fun shade(hit: Intersection, scene: Scene, bounce: Int): Vector3f {
        return scene.lights.fold(Vector3f()) { acc, light ->
            val lightDir = light.getDirection(hit.point)

            //backfacing rejection
            val nDotL = hit.normal dot lightDir
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

            val color = hit.material.texture?.sample(hit.uv ?: Vector2(0f, 0f))
                ?: hit.material.color

            acc + color.toVector3() * intensity
        }
    }
}

class SpecularStage : ShadingStage {
    override fun shade(hit: Intersection, scene: Scene, bounce: Int): Vector3 {
        // Simple Phong specular model (placeholder)
        return scene.lights.fold (Vector3(0f, 0f, 0f)) { acc, light ->
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

    private fun reflect(dir: Vector3, normal: Vector3): Vector3 {
        return dir - normal * 2f * dir.dot(normal)
    }
}
