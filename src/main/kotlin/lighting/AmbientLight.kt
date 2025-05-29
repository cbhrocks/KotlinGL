package org.kotlingl.lighting

import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lights.Light
import org.kotlingl.math.Vector3
import org.kotlingl.math.times
import org.kotlingl.math.toVector3

class AmbientLight(
    override val color: ColorRGB,
    override val brightness: Float
) : Light {
    override val contributesToShadows: Boolean
        get() = false

    override fun getDirection(toPoint: Vector3): Vector3 {
        return Vector3.ZERO
    }

    override fun computeIntensity(hit: Intersection, direction: Vector3): Vector3 {
        return hit.material.color.toVector3() * color.toVector3() * brightness
    }

    override fun getDistance(point: Vector3): Float {
        return 0f
    }
}