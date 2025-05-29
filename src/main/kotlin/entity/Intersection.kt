package org.kotlingl.entity

import org.kotlingl.math.Vector2
import org.kotlingl.math.Vector3

data class Intersection(
    val point: Vector3,        // Point of intersection in world space
    val normal: Vector3,       // Surface normal at that point (should be normalized)
    val t: Float,              // Distance along the ray where the hit occurred
    val material: Material,    // Material of the surface hit
    val frontFace: Boolean,     // Whether the intersection is on the outside of the object
    val uv: Vector2? = null   // Optional: texture coordinates
)
