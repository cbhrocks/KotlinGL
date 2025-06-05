package org.kotlingl.math

import org.joml.Vector3f

val EPSILON: Float
    get() = 1e-4f


// These create new vectors. If you're working in performance-critical code (like in a game loop), you may want to
// reuse vectors instead of allocating new ones every time.

operator fun Vector3f.plus(other: Vector3f): Vector3f =
    Vector3f(this).add(other)

operator fun Vector3f.minus(other: Vector3f): Vector3f =
    Vector3f(this).sub(other)

operator fun Vector3f.times(scalar: Float): Vector3f =
    Vector3f(this).mul(scalar)
operator fun Vector3f.times(v: Vector3f): Vector3f =
    Vector3f(this).mul(v)

operator fun Vector3f.div(scalar: Float): Vector3f =
    Vector3f(this).div(scalar)

operator fun Vector3f.unaryMinus(): Vector3f =
    Vector3f(this).negate()

fun Vector3f.directionTo(target: Vector3f): Vector3f {
    return Vector3f(target).sub(this).normalize()
}
fun Vector3f.distanceTo(target: Vector3f): Vector3f {
    return Vector3f(target).sub(this)
}

