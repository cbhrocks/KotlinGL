package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.kotlingl.entity.Intersection
import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.AABB.Companion.surroundingBox
import org.kotlingl.shapes.Bounded
import org.kotlingl.shapes.Ray

class BVHTree(val root: BVHNode) {
    private fun pickClosest(hitL: Intersection?, hitR: Intersection?): Intersection? {
        return when {
            hitL == null -> hitR
            hitR == null -> hitL
            hitL.t < hitR.t -> hitL
            else -> hitR
        }
    }

    fun intersects(ray: Ray, node: BVHNode = root): Intersection? {
        if (!node.aabb.intersects(ray)) return null

        return when (node) {
            is BVHLeaf -> {
                // Transform the ray to local space if needed
                val localRay = node.modelMatrix?.let { ray.transformedBy(Matrix4f(it).invert()) } ?: ray
                node.geometry.intersects(localRay)?.transformedBy(node.modelMatrix)
            }

            is BVHInternal -> {
                val hitLeft = intersects(ray, node.left)
                val hitRight = intersects(ray, node.right)
                pickClosest(hitLeft, hitRight)
            }

            is BVHGroup -> {
                // Transform ray into group's local space if needed
                val localRay = node.modelMatrix?.let { ray.transformedBy(Matrix4f(it).invert()) } ?: ray
                var closestHit: Intersection? = null
                for (child in node.children) {
                    val hit = intersects(localRay, child)?.transformedBy(node.modelMatrix)
                    closestHit = pickClosest(closestHit, hit)
                }
                closestHit
            }
        }
    }

    companion object {
        fun buildForModel(model: Model): BVHTree {
            return BVHTree(buildBVHForModel(model))
        }

        private fun buildBVHForModel(model: Model): BVHNode {
            val childNodes = mutableListOf<BVHNode>()

            // Create leaf nodes for each mesh
            for (mesh in model.meshes) {
                val localAABB = mesh.computeAABB()
                val worldAABB = model.modelM.let { localAABB.transformedBy(it) }
                childNodes.add(BVHLeaf(worldAABB, mesh, model.modelM))
            }

            // Recursively build child BVH groups
            for (childModel in model.children) {
                childNodes.add(buildBVHForModel(childModel))
            }

            // Wrap in BVHGroup if necessary
            return if (childNodes.size == 1) {
                childNodes[0] // Pass through
            } else {
                val aabb = childNodes.map { it.aabb }.reduce(::surroundingBox)
                BVHGroup(aabb, childNodes, model.modelM)
            }
        }
    }
}

sealed class BVHNode {
    abstract var aabb: AABB
}

data class BVHLeaf(
    override var aabb: AABB,
    val geometry: Bounded,
    val modelMatrix: Matrix4f? = null
) : BVHNode()

data class BVHInternal(
    override var aabb: AABB,
    val left: BVHNode,
    val right: BVHNode
) : BVHNode()

data class BVHGroup(
    override var aabb: AABB,
    val children: List<BVHNode>,
    val modelMatrix: Matrix4f? = null
) : BVHNode()


///**
// * @param localAABB This is the AABB generated around local vertices
// * @param modelMatrix The model matrix necessary to translate the localAABB into world space
// * @param worldAABB This is the world space translation of the local AABB using the modelMatrix
// */
//class BVHNodeOld (
//    val localAABB: AABB,
//    val left: BVHNode? = null,
//    val right: BVHNode? = null,
//    val leaf: Bounded? = null,
//    val modelMatrix: Matrix4fc? = null,
//    val modelMatrixInverse: Matrix4fc? = null
//){
//    var worldAABB: AABB = modelMatrix?.let{ localAABB.transformedBy(it) } ?: localAABB
//
//    /**
//     * Call this if the modelMatrix is updated (e.g. in animation)
//     */
//    fun updateWorldAABB() {
//        worldAABB = modelMatrix?.let { localAABB.transformedBy(it) } ?: localAABB
//    }
//
//    fun intersects(ray: Ray): Intersection? {
//        val localRay = modelMatrixInverse?.let {
//            ray.transformedBy(it)
//        } ?: ray
//
//        if (!this.localAABB.intersect(ray)) return null
//        if (this.leaf != null) return this.leaf.intersects(ray)
//
//        val hitL = this.left?.intersects(ray)
//        val hitR = this.right?.intersects(ray)
//
//        return when {
//            hitL != null && hitR != null -> if (hitL.t < hitR.t) hitL else hitR
//            hitL != null -> hitL
//            else -> hitR
//        }
//    }
//
//    companion object {
//        fun fromBounded(
//            bounded: List<Bounded>,
//        ): BVHNode {
//            if (bounded.size == 1) {
//                val aabb = bounded[0].computeAABB()
//                return BVHNode(aabb, leaf = bounded[0])
//            }
//
//            // Compute bounding box for all
//            val globalAABB = bounded.map {
//                it.computeAABB()
//            }
//                .reduce { acc, aabb -> AABB.surroundingBox(acc, aabb) }
//
//            // Split on longest axis
//            val axis = globalAABB.largestAxis()
//            val sorted = bounded.sortedBy { it.centroid()[axis] }
//            val mid = sorted.size / 2
//
//            val left = fromBounded(sorted.subList(0, mid))
//            val right = fromBounded(sorted.subList(mid, sorted.size))
//
//            return BVHNode(
//                AABB.surroundingBox(left.localAABB, right.localAABB),
//                left,
//                right,
//            )
//        }
//    }
//}
