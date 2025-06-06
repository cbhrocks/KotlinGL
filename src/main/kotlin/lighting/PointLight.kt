package org.kotlingl.lighting

import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.minus
import org.joml.times
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lights.Light

class PointLight(
    override val color: ColorRGB,
    val position: Vector3f,
    override val brightness: Float,
    override var contributesToShadows: Boolean = true
): Light {

    override fun getDirection(toPoint: Vector3fc): Vector3f {
        return (position - toPoint).normalize()
    }

    override fun computeIntensity(hit: Intersection, direction: Vector3fc): Vector3f {
        val nDotL = maxOf(hit.normal.dot(direction), 0f)
        return color.toVector3f() * brightness * nDotL
    }

    override fun getDistance(point: Vector3fc): Float {
        return position.distance(point)
    }
}