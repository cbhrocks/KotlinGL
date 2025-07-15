package org.kotlingl.Collider

import org.joml.Vector3f
import org.kotlingl.math.TrackedMatrix

object ColliderFactory {

    fun createBox(transform: TrackedMatrix, size: Vector3f): Collider {
        return BoxCollider(transform, size)
    }

    fun createCapsule(shared: TrackedMatrix, height: Float, radius: Float): Collider {
        return CapsuleCollider(shared, height, radius)
    }

    // etc.
}