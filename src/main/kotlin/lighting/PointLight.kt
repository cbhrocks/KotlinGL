package org.kotlingl.lighting

import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lights.Light
import org.kotlingl.math.*

class PointLight(
    override val color: ColorRGB,
    val position: Vector3,
    override val brightness: Float,
    override var contributesToShadows: Boolean = true
): Light {

    override fun getDirection(toPoint: Vector3): Vector3 {
        return toPoint.directionTo(position)
    }

    override fun computeIntensity(hit: Intersection, direction: Vector3): Vector3 {
        val ndotl = maxOf(hit.normal.dot(direction), 0f)
        return color.toVector3() * brightness * ndotl
    }

    override fun getDistance(point: Vector3): Float {
        return position.distanceTo(point)
    }
}