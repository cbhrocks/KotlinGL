package org.kotlingl.lighting

import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.times
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lights.Light

class AmbientLight(
    override val color: ColorRGB,
    override val brightness: Float
) : Light {
    override val contributesToShadows: Boolean
        get() = false

    override fun getDirection(toPoint: Vector3fc): Vector3f {
        return Vector3f(0f, 1f, 0f)
    }

    override fun computeIntensity(hit: Intersection, direction: Vector3fc): Vector3f {
        return hit.material.baseColor.toVector3f() * color.toVector3f() * brightness
    }

    override fun getDistance(point: Vector3fc): Float {
        return 1f
    }
}