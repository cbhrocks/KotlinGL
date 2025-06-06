package org.kotlingl.lights

import org.joml.Vector3f
import org.joml.Vector3fc
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection

interface Light {
    val color: ColorRGB
    val brightness: Float
    val contributesToShadows: Boolean

    /**
     * The direction from the surface toward the light.
     */
    fun getDirection(toPoint: Vector3fc): Vector3f

    /**
     * computes the RGB intensity the light has on the hit point.
     * @param hit The data class representing the hitpoint of the ray being traced with an object
     * @param direction the direction from the hitpoint towards the light
     */
    fun computeIntensity(hit: Intersection, direction: Vector3fc): Vector3f;

    /**
     * returns the distance from the point param to the light.
     * @param point the point used for distance
     * @return returns a float representing distance. Could be Float.POSITIVE_INFINITY for when lights don't have a
     * position.
     */
    fun getDistance(point: Vector3fc): Float

}