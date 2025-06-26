package org.kotlingl.entity

import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector2fc
import org.joml.Vector3f
import org.joml.Vector3fc

data class Intersection(
    val point: Vector3fc,        // Point of intersection in world space
    val normal: Vector3fc,       // Surface normal at that point (should be normalized)
    val t: Float,              // Distance along the ray where the hit occurred
    val material: Material,    // Material of the surface hit
    val frontFace: Boolean,     // Whether the intersection is on the outside of the object
    val uv: Vector2fc? = null   // Optional: texture coordinates
) {
    fun transformedBy(matrix: Matrix4f?): Intersection? {
        if (matrix == null) return this

        val newPoint = matrix.transformPosition(Vector3f(point))

        // Correctly transform the normal using inverse-transpose of the upper-left 3x3 matrix
        val normalMatrix = Matrix3f().set(matrix).invert().transpose()
        val newNormal = normalMatrix.transform(Vector3f(normal)).normalize()

        return this.copy(
            point = newPoint,
            normal = newNormal
        )
    }
}
