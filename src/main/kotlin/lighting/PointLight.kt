package org.kotlingl.lighting

import org.joml.Vector3f
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lights.Light
import org.kotlingl.math.*

class PointLight(
    override val color: ColorRGB,
    val position: Vector3f,
    override val brightness: Float,
    override var contributesToShadows: Boolean = true
): Light {

    override fun getDirection(toPoint: Vector3f): Vector3f {
        return toPoint.directionTo(position)
    }

    override fun computeIntensity(hit: Intersection, direction: Vector3f): Vector3f {
        val ndotl = maxOf(hit.normal.dot(direction), 0f)
        return color.toVector3f() * brightness * ndotl
    }

    override fun getDistance(point: Vector3f): Float {
        return position.distance(point)
    }
}