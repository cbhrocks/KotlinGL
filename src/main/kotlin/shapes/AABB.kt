package org.kotlingl.shapes

import org.joml.Vector3f

data class AABB(val min: Vector3f, val max: Vector3f) {
    fun intersect(
        ray: Ray,
        tMin: Float = 0.001f,
        tMax: Float = Float.POSITIVE_INFINITY
    ): Boolean {
        var t0 = tMin
        var t1 = tMax

        for (i in 0..2) {
            val invD = 1f / ray.direction[i]
            var tNear = (min[i] - ray.origin[i]) * invD
            var tFar = (max[i] - ray.origin[i]) * invD

            if (invD < 0f) {
                val temp = tNear
                tNear = tFar
                tFar = temp
            }

            t0 = maxOf(t0, tNear)
            t1 = minOf(t1, tFar)

            if (t1 <= t0) return false
        }

        return true
    }

    fun largestAxis(): Int {
        // extent means size
        val extent = max.sub(min, Vector3f())  // extent = max - min
        return when {
            extent.x >= extent.y && extent.x >= extent.z -> 0
            extent.y >= extent.z -> 1
            else -> 2
        }
    }

    companion object {
        // this is for creating a box that contains other boxes
        fun surroundingBox(box0: AABB, box1: AABB): AABB {
            val small = Vector3f(
                minOf(box0.min.x, box1.min.x),
                minOf(box0.min.y, box1.min.y),
                minOf(box0.min.z, box1.min.z)
            )
            val big = Vector3f(
                maxOf(box0.max.x, box1.max.x),
                maxOf(box0.max.y, box1.max.y),
                maxOf(box0.max.z, box1.max.z)
            )
            return AABB(small, big)
        }
    }
}