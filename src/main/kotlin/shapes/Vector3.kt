package org.kotlingl.shapes

data class Vector3(val x: Float, val y: Float, val z: Float) {

    val r: Int
        get() {
            return (this.x * 255f).toInt().coerceIn(0, 255)
        }
    val g: Int
        get() {
            return (this.y * 255f).toInt().coerceIn(0, 255)
        }
    val b: Int
        get() {
            return (this.z * 255f).toInt().coerceIn(0, 255)
        }

    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)

    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)

    override fun toString() = "(${this.x}, ${this.y}, ${this.z})"

    infix fun dot(other: Vector3): Float =
        x * other.x + y * other.y + z * other.z

    infix fun cross(other: Vector3): Vector3 =
        Vector3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )

    fun length(): Float = kotlin.math.sqrt(this dot this)

    fun normalize(): Vector3 {
        val len = length()
        return if (len == 0f) this else this / len
    }

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val UNIT_X = Vector3(1f, 0f, 0f)
        val UNIT_Y = Vector3(0f, 1f, 0f)
        val UNIT_Z = Vector3(0f, 0f, 1f)
    }
}
