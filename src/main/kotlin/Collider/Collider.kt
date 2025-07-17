package org.kotlingl.Collider

import org.joml.Vector2f
import org.joml.Vector3f
import org.kotlingl.math.TrackedMatrix

abstract class Collider(val transform: TrackedMatrix, val layer: Int, val mask: Int) {
    abstract val shape: ColliderShape

    fun overlaps(other: Collider): Boolean {
        return Intersector.intersects(this.shape, other.shape)
    }

    fun shouldCheckCollisionsWith(other: Collider): Boolean {
        return (this.mask and other.layer) != 0 && (other.mask and this.layer) != 0
    }
}

class BoxCollider(
    transform: TrackedMatrix,
    val halfSize: Vector2f,
    layer: Int,
    mask: Int
) : Collider(
    transform,
    layer,
    mask
) {
    override var shape: OBB2D = extractOBB2D()

    fun update() {
        shape = extractOBB2D()
    }

    private fun extractOBB2D(): OBB2D {
        val m = transform.getRef()

        val center = Vector2f(m.m30(), m.m31())
        val xAxis = Vector2f(m.m00(), m.m01()).normalize()
        val yAxis = Vector2f(m.m10(), m.m11()).normalize()

        return OBB2D(center, halfSize, arrayOf(xAxis, yAxis))
    }
}

class CircleCollider(transform: TrackedMatrix, val radius: Float, layer: Int, mask: Int) : Collider(transform, layer,
    mask
) {
    override var shape = extractCircle()

    fun update() {
        shape = extractCircle()
    }

    private fun extractCircle(): Circle {
        val m = transform.getRef()

        val center = Vector2f(m.m30(), m.m31())

        return Circle(center, radius)
    }
}

class CapsuleCollider(
    transform: TrackedMatrix,
    val radius: Float,
    val height: Float,
    layer: Int,
    mask: Int
) : Collider(
    transform,
    layer,
    mask
) {
    override var shape = extractCapsule()

    fun update() {
        shape = extractCapsule()
    }

    private fun extractCapsule(): Capsule {
        val m = transform.getRef()

        // Define local segment in object space (Y-aligned)
        val halfHeight = height / 2f
        val localStart = Vector3f(0f, -halfHeight, 0f)
        val localEnd = Vector3f(0f, halfHeight, 0f)

        // Transform to world space
        val worldStart = localStart.mulPosition(m)
        val worldEnd = localEnd.mulPosition(m)

        // Convert to 2D XY
        val p0 = Vector2f(worldStart.x, worldStart.y)
        val p1 = Vector2f(worldEnd.x, worldEnd.y)

        return Capsule(p0, p1, radius)
    }
}
