package org.kotlingl.model

import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.Triangle

data class BVHNode(
    val boundingBox: AABB,
    val left: BVHNode? = null,
    val right: BVHNode? = null,
    val triangle: Triangle? = null
)

class BVH(val root: BVHNode) {
    companion object {
        fun build(triangles: List<Triangle>): BVH {
            val rootNode = buildRecursive(triangles)
            return BVH(rootNode)
        }

        private fun buildRecursive(triangles: List<Triangle>): BVHNode {
            if (triangles.size == 1) {
                val aabb = triangles[0].computeAABB()
                return BVHNode(aabb, triangle = triangles[0])
            }

            // Compute bounding box for all
            val globalAABB = triangles.map { it.computeAABB() }
                .reduce { acc, aabb -> AABB.surroundingBox(acc, aabb) }

            // Split on longest axis
            val axis = globalAABB.largestAxis()
            val sorted = triangles.sortedBy { it.centroid()[axis] }
            val mid = sorted.size / 2

            val left = buildRecursive(sorted.subList(0, mid))
            val right = buildRecursive(sorted.subList(mid, sorted.size))

            return BVHNode(AABB.surroundingBox(left.boundingBox, right.boundingBox), left, right)
        }
    }
}