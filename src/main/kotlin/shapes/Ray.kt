package org.kotlingl.shapes

import org.joml.Matrix4f
import org.joml.Vector3f

data class Ray(
    val origin: Vector3f = Vector3f(0f, 0f, 0f),
    val direction: Vector3f = Vector3f(1f, 0f, 0f)
) {
    fun transformedBy(matrix: Matrix4f): Ray {
        // Transform origin as a point (affected by translation)
        val newOrigin = origin.mulPosition(matrix, Vector3f())

        // Transform direction as a vector (no translation)
        val newDirection = direction.mulDirection(matrix, Vector3f()).normalize()

        return Ray(newOrigin, newDirection)
    }
}