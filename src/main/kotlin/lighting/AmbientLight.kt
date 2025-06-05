package org.kotlingl.lighting

import org.joml.Vector3f
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lights.Light
import org.kotlingl.math.times

class AmbientLight(
    override val color: ColorRGB,
    override val brightness: Float
) : Light {
    override val contributesToShadows: Boolean
        get() = false

    override fun getDirection(toPoint: Vector3f): Vector3f {
        return Vector3f()
    }

    override fun computeIntensity(hit: Intersection, direction: Vector3f): Vector3f {
        return hit.material.color.toVector3f() * color.toVector3f() * brightness
    }

    override fun getDistance(point: Vector3f): Float {
        return 0f
    }
}