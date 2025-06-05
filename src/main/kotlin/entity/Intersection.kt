package org.kotlingl.entity

import org.joml.Vector2f
import org.joml.Vector3f

data class Intersection(
    val point: Vector3f,        // Point of intersection in world space
    val normal: Vector3f,       // Surface normal at that point (should be normalized)
    val t: Float,              // Distance along the ray where the hit occurred
    val material: Material,    // Material of the surface hit
    val frontFace: Boolean,     // Whether the intersection is on the outside of the object
    val uv: Vector2f? = null   // Optional: texture coordinates
)
