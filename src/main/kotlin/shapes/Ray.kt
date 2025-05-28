package org.kotlingl.shapes

import org.kotlingl.math.Vector3

data class Ray(
    val origin: Vector3 = Vector3(0f, 0f, 0f),
    val direction: Vector3 = Vector3(1f, 0f, 0f)
)