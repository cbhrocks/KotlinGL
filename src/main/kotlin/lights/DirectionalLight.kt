package org.kotlingl.lights

import org.kotlingl.shapes.Vector3

class DirectionalLight(
    override val Color: Vector3,
    override val origin: Vector3
) : Light {

    override fun getIntensity(position: Vector3): Float {
        return 1f
    }
}