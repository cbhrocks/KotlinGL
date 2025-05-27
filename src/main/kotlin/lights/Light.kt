package org.kotlingl.lights

import org.kotlingl.shapes.Vector3

interface Light {
    val Color: Vector3
    val origin: Vector3
    /**
     * takes in a color, usually the color of an object, and returns a new color based
     * on the properties of the light
     */
    fun getIntensity(position: Vector3): Float;
}