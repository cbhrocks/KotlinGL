package org.kotlingl.lights

import org.kotlingl.entity.ColorRGB
import org.kotlingl.math.Vector3

interface Light {
    val color: ColorRGB
    val origin: Vector3
    /**
     * takes in a color, usually the color of an object, and returns a new color based
     * on the properties of the light
     */
    fun getIntensity(position: Vector3): Float;
}