package org.kotlingl.Collider

import org.joml.Vector3f
import org.kotlingl.math.TrackedMatrix

abstract class Collider(val transform: TrackedMatrix) {
    abstract fun updatePhysicsShape()
}

class BoxCollider(transform: TrackedMatrix, val size: Vector3f) : Collider(transform) {
    override fun updatePhysicsShape() {
        val transform = transform.getRef()
        // Push to physics engine here
    }
}

class CapsuleCollider(transform: TrackedMatrix, val height: Float, val radius: Float) : Collider(transform) {
    override fun updatePhysicsShape() {
        val transform = transform.getRef()
        // Push to physics engine here
    }
}
