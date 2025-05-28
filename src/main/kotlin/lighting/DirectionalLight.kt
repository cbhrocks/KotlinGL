package org.kotlingl.lights

import org.kotlingl.entity.ColorRGB
import org.kotlingl.math.Vector3

class DirectionalLight(
    override val color: ColorRGB,
    override val origin: Vector3
) : Light {

    override fun getIntensity(position: Vector3): Float {
        return 1f
    }
}