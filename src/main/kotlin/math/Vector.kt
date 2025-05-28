package org.kotlingl.math

import org.kotlingl.entity.ColorRGB

class Vector2(val x: Float, val y: Float) {
    companion object {
        val ZERO = Vector2(0f, 0f)
        val UNIT_X = Vector2(1f, 0f)
        val UNIT_Y = Vector2(0f, 1f)
    }
}
class Vector3(val x: Float, val y: Float, val z: Float) {
    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val UNIT_X = Vector3(1f, 0f, 0f)
        val UNIT_Y = Vector3(0f, 1f, 0f)
        val UNIT_Z = Vector3(0f, 0f, 1f)
    }
}

operator fun Vector3.plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
operator fun Vector2.plus(other: Vector2) = Vector2(x + other.x, y + other.y)

operator fun Vector3.minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
operator fun Vector2.minus(other: Vector2) = Vector2(x - other.x, y - other.y)

operator fun Vector3.times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
operator fun Vector2.times(scalar: Float) = Vector2(x * scalar, y * scalar)

operator fun Vector3.div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)
operator fun Vector2.div(scalar: Float) = Vector2(x / scalar, y / scalar)

fun Vector3.toString() = "(${this.x}, ${this.y}, ${this.z})"
fun Vector2.toString() = "(${this.x}, ${this.y})"

infix fun Vector3.dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z
infix fun Vector2.dot(other: Vector2): Float = x * other.x + y * other.y

infix fun Vector3.cross(other: Vector3): Vector3 =
    Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
infix fun Vector2.cross(other: Vector2): Float = x * other.y - y * other.x

fun Vector3.length(): Float = kotlin.math.sqrt(this dot this)
fun Vector2.length(): Float = kotlin.math.sqrt(this dot this)

fun Vector3.normalize(): Vector3 {
    val len = length()
    return if (len == 0f) this else this / len
}
fun Vector2.normalize(): Vector2 {
    val len = length()
    return if (len == 0f) this else this / len
}

fun ColorRGB.toVector3(): Vector3 = Vector3(r / 255f, g / 255f, b / 255f)
