package org.kotlingl.Collider

import org.joml.Vector2f
import kotlin.math.abs

object Intersector {
    fun intersects(a: ColliderShape, b: ColliderShape): Boolean {
        return when {
            a is OBB2D && b is OBB2D -> obbVsObb(a, b)
            a is Circle && b is Circle -> circleVsCircle(a, b)
            a is Circle && b is OBB2D -> circleVsObb(a, b)
            a is OBB2D && b is Circle -> circleVsObb(b, a)
            a is Capsule && b is Circle -> capsuleVsCircle(a, b)
            a is Circle && b is Capsule -> capsuleVsCircle(b, a)
            a is Capsule && b is OBB2D -> capsuleVsObb(a, b)
            a is OBB2D && b is Capsule -> capsuleVsObb(b, a)
            a is Capsule && b is Capsule -> capsuleVsCapsule(a, b)
            else -> error("Unsupported shape combination: ${a::class} vs ${b::class}")
        }
    }

    // --- Core Intersection Functions ---

    fun obbVsObb(a: OBB2D, b: OBB2D): Boolean {
        for (axis in a.axes + b.axes) {
            if (!overlapOnAxis(a, b, axis)) return false
        }
        return true
    }

    private fun overlapOnAxis(a: OBB2D, b: OBB2D, axis: Vector2f): Boolean {
        val projectionA = projectOntoAxis(a, axis)
        val projectionB = projectOntoAxis(b, axis)
        return projectionA.overlaps(projectionB)
    }

    private fun projectOntoAxis(obb: OBB2D, axis: Vector2f): Projection {
        val centerProj = obb.center.dot(axis)
        val extent = obb.halfExtents.x * abs(obb.axes[0].dot(axis)) +
                obb.halfExtents.y * abs(obb.axes[1].dot(axis))
        return Projection(centerProj - extent, centerProj + extent)
    }

    data class Projection(val min: Float, val max: Float) {
        fun overlaps(other: Projection): Boolean {
            return !(this.max < other.min || this.min > other.max)
        }
    }

    fun circleVsCircle(a: Circle, b: Circle): Boolean {
        val distSq = a.center.distanceSquared(b.center)
        val radiusSum = a.radius + b.radius
        return distSq <= radiusSum * radiusSum
    }

    fun circleVsObb(circle: Circle, obb: OBB2D): Boolean {
        val dir = Vector2f(circle.center).sub(obb.center)

        // Project dir onto OBB's local axes
        val localX = obb.axes[0]
        val localY = obb.axes[1]

        val dx = clamp(dir.dot(localX), -obb.halfExtents.x, obb.halfExtents.x)
        val dy = clamp(dir.dot(localY), -obb.halfExtents.y, obb.halfExtents.y)

        val closest = Vector2f(obb.center)
            .fma(dx, localX)
            .fma(dy, localY)

        return closest.distanceSquared(circle.center) <= circle.radius * circle.radius
    }

    fun capsuleVsCircle(capsule: Capsule, circle: Circle): Boolean {
        val closest = closestPointOnSegment(circle.center, capsule.start, capsule.end)
        val totalRadius = capsule.radius + circle.radius
        return closest.distanceSquared(circle.center) <= totalRadius * totalRadius
    }

    fun capsuleVsObb(capsule: Capsule, obb: OBB2D): Boolean {
        // Approximate: sample capsule segment as line and test multiple points
        val samples = 5
        for (i in 0..samples) {
            val t = i.toFloat() / samples
            val p = Vector2f(capsule.start).lerp(capsule.end, t)
            val circle = Circle(p, capsule.radius)
            if (circleVsObb(circle, obb)) return true
        }
        return false
    }

    fun capsuleVsCapsule(a: Capsule, b: Capsule): Boolean {
        val p1 = closestPointOnSegmentSegment(a.start, a.end, b.start, b.end)
        val p2 = closestPointOnSegmentSegment(b.start, b.end, a.start, a.end)
        val distSq = p1.distanceSquared(p2)
        val rSum = a.radius + b.radius
        return distSq <= rSum * rSum
    }

    // --- Helpers ---

    fun closestPointOnSegment(p: Vector2f, a: Vector2f, b: Vector2f): Vector2f {
        val ab = Vector2f(b).sub(a)
        val t = clamp(Vector2f(p).sub(a).dot(ab) / ab.lengthSquared(), 0f, 1f)
        return Vector2f(a).fma(t, ab)
    }

    fun closestPointOnSegmentSegment(a1: Vector2f, a2: Vector2f, b1: Vector2f, b2: Vector2f): Vector2f {
        // Simplified: project b1 onto a segment, better approximations possible
        return closestPointOnSegment(b1, a1, a2)
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return kotlin.math.max(min, kotlin.math.min(value, max))
    }
}