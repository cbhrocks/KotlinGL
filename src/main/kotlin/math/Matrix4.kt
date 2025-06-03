package org.kotlingl.math


import kotlin.math.*

class Matrix4(private val m: FloatArray) {

    companion object {
        val IDENTITY: Matrix4 = Matrix4(
            floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            )
        )

        fun translation(x: Float, y: Float, z: Float): Matrix4 = Matrix4(
            floatArrayOf(
                1f, 0f, 0f, x,
                0f, 1f, 0f, y,
                0f, 0f, 1f, z,
                0f, 0f, 0f, 1f
            )
        )

        fun scale(x: Float, y: Float, z: Float): Matrix4 = Matrix4(
            floatArrayOf(
                x, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                0f, 0f, z, 0f,
                0f, 0f, 0f, 1f
            )
        )

        fun rotationX(angleRad: Float): Matrix4 {
            val c = cos(angleRad)
            val s = sin(angleRad)
            return Matrix4(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, c, -s, 0f,
                    0f, s, c, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }

        fun rotationY(angleRad: Float): Matrix4 {
            val c = cos(angleRad)
            val s = sin(angleRad)
            return Matrix4(
                floatArrayOf(
                    c, 0f, s, 0f,
                    0f, 1f, 0f, 0f,
                    -s, 0f, c, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }

        fun rotationZ(angleRad: Float): Matrix4 {
            val c = cos(angleRad)
            val s = sin(angleRad)
            return Matrix4(
                floatArrayOf(
                    c, -s, 0f, 0f,
                    s, c, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }
    }

    // Matrix multiplication
    operator fun times(other: Matrix4): Matrix4 {
        val result = FloatArray(16)
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                result[row * 4 + col] = (0 until 4).sumOf { i ->
                    m[row * 4 + i].toDouble() * other.m[i * 4 + col].toDouble()
                }.toFloat()
            }
        }
        return Matrix4(result)
    }

    fun transform(vec: Vector3): Vector3 {
        val x = vec.x
        val y = vec.y
        val z = vec.z
        val w = 1f
        val nx = m[0] * x + m[1] * y + m[2] * z + m[3] * w
        val ny = m[4] * x + m[5] * y + m[6] * z + m[7] * w
        val nz = m[8] * x + m[9] * y + m[10] * z + m[11] * w
        val nw = m[12] * x + m[13] * y + m[14] * z + m[15] * w
        return Vector3(nx / nw, ny / nw, nz / nw)
    }

    fun toFloatArray(): FloatArray = m.copyOf()
}
