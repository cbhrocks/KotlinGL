package org.kotlingl.Collider

import org.joml.Vector2f


interface ColliderShape

data class OBB2D (
    val center: Vector2f,
    val halfExtents: Vector2f,
    val axes: Array<Vector2f>
): ColliderShape

data class Circle (
    val center: Vector2f,
    val radius: Float,
): ColliderShape

data class Capsule(
    val start: Vector2f,
    val end: Vector2f,
    val radius: Float
) : ColliderShape
