package org.kotlingl.math

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.assimp.AIMatrix4x4

val EPSILON: Float
    get() = 1e-4f

fun AIMatrix4x4.toJoml(): Matrix4f {
    return Matrix4f(
        this.a1(), this.b1(), this.c1(), this.d1(),
        this.a2(), this.b2(), this.c2(), this.d2(),
        this.a3(), this.b3(), this.c3(), this.d3(),
        this.a4(), this.b4(), this.c4(), this.d4()
    )
}

//infix fun Vector3f.dot(other: Vector3f) = Vector3f.dot(other)

fun Vector3f.toFloatArray(): FloatArray {
    return floatArrayOf(this.x, this.y, this.z)
}

fun Quaternionf.toFloatArray(): FloatArray {
    return floatArrayOf(this.x, this.y, this.z, this.w)
}