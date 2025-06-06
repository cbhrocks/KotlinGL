package org.kotlingl.lights

import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.times
import org.joml.unaryMinus
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection

class DirectionalLight(
    override val color: ColorRGB,
    val direction: Vector3f,
    override val brightness: Float,
    override var contributesToShadows: Boolean = true
) : Light {
    override fun getDirection(toPoint: Vector3fc): Vector3f {
        return -direction.normalize()
    }

    override fun computeIntensity(hit: Intersection, direction: Vector3fc): Vector3f {
        val ndotl = maxOf(hit.normal.dot(direction), 0f)
        return color.toVector3f() * brightness * ndotl
    }

    override fun getDistance(point: Vector3fc): Float {
        return Float.POSITIVE_INFINITY
    }
}