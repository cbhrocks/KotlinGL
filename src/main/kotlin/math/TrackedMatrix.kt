package org.kotlingl.math

import org.joml.Matrix4f

class TrackedMatrix(private val matrix: Matrix4f = Matrix4f()) {
    var isDirty = true
        private set

    fun mutate(action: Matrix4f.() -> Unit): TrackedMatrix {
        matrix.action()
        isDirty = true
        return this
    }

    fun markClean() {
        isDirty = false
    }

    fun get(): Matrix4f = Matrix4f(matrix)

    fun set(matrix: Matrix4f): TrackedMatrix {
        this.matrix.set(matrix)
        isDirty = true
        return this
    }

    fun getRef(): Matrix4f = matrix
}