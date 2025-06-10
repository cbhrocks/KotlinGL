package org.kotlingl.model

import org.kotlingl.entity.Intersection
import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.Bounded
import org.kotlingl.shapes.Ray

class BVHNode (
    val boundingBox: AABB,
    val left: BVHNode? = null,
    val right: BVHNode? = null,
    val leaf: Bounded? = null,

){
    fun intersects(ray: Ray): Intersection? {
        if (!this.boundingBox.intersect(ray)) return null
        if (this.leaf != null) return this.leaf.intersects(ray)

        val hitL = this.left?.intersects(ray)
        val hitR = this.right?.intersects(ray)

        return when {
            hitL != null && hitR != null -> if (hitL.t < hitR.t) hitL else hitR
            hitL != null -> hitL
            else -> hitR
        }
    }

    companion object {
        fun fromBounded(bounded: List<Bounded>): BVHNode {
            if (bounded.size == 1) {
                val aabb = bounded[0].computeAABB()
                return BVHNode(aabb, leaf = bounded[0])
            }

            // Compute bounding box for all
            val globalAABB = bounded.map { it.computeAABB() }
                .reduce { acc, aabb -> AABB.surroundingBox(acc, aabb) }

            // Split on longest axis
            val axis = globalAABB.largestAxis()
            val sorted = bounded.sortedBy { it.centroid()[axis] }
            val mid = sorted.size / 2

            val left = fromBounded(sorted.subList(0, mid))
            val right = fromBounded(sorted.subList(mid, sorted.size))

            return BVHNode(
                AABB.surroundingBox(left.boundingBox, right.boundingBox),
                left,
                right
            )
        }
    }
}
