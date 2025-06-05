package org.kotlingl.lights

import org.joml.Vector3f
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.math.*

class DirectionalLight(
    override val color: ColorRGB,
    val direction: Vector3f,
    override val brightness: Float,
    override var contributesToShadows: Boolean = true
) : Light {
    override fun getDirection(toPoint: Vector3f): Vector3f {
        return -direction.normalize()
    }

    override fun computeIntensity(hit: Intersection, direction: Vector3f): Vector3f {
        val ndotl = maxOf(hit.normal.dot(direction), 0f)
        return color.toVector3f() * brightness * ndotl
    }

    override fun getDistance(point: Vector3f): Float {
        return Float.POSITIVE_INFINITY
    }
}