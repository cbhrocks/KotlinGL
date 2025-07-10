package org.kotlingl.model

import org.kotlingl.entity.Intersection
import org.kotlingl.math.TrackedMatrix
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
        val localRay = when(node) {
            is BVHGroup -> {
                node.modelMatrix?.let { ray.transformedBy(it.get().invert()) } ?: ray
            }
            is BVHLeaf -> {
                node.modelMatrix?.let { ray.transformedBy(it.get().invert()) } ?: ray
            }
            is BVHInternal -> {
                ray
            }
        }

        if (!node.aabb.intersects(localRay)) return null

        return when (node) {
            is BVHLeaf -> {
                // Transform the ray to local space if needed
                node.geometry.intersects(localRay)?.transformedBy(node.modelMatrix?.getRef())
            }
            is BVHInternal -> {
                val hitLeft = intersects(ray, node.left)
                val hitRight = intersects(ray, node.right)
                pickClosest(hitLeft, hitRight)
            }
            is BVHGroup -> {
                // Transform ray into group's local space if needed
                var closestHit: Intersection? = null
                for (child in node.children) {
                    val hit = intersects(localRay, child)?.transformedBy(node.modelMatrix?.getRef())
                    closestHit = pickClosest(closestHit, hit)
                }
                closestHit
            }
        }
    }

    fun refit() {
        root.recomputeAABBIfNeeded()
        root.markClean()
    }

    companion object {
        fun buildForModel(model: Model): BVHTree {
            val bvhTree = buildBVHForModel(model, model.skeleton.rootId)
            require (bvhTree != null) { "BVHTree is empty! model must not have any geometry." }
            return BVHTree(bvhTree)
        }

        private fun buildBVHForModel(model: Model, currentNodeId: Int): BVHNode? {
            val skeletonNode = model.skeleton.nodeMap.getValue(currentNodeId)

            val childNodes = mutableListOf<BVHNode>()

            // Create leaf nodes for each mesh
            for (meshIndex in model.nodeIdToMeshIndices.getValue(skeletonNode.id)) {
                val mesh = model.meshes[meshIndex]
                val localAABB = mesh.computeAABB()
                //val worldAABB = model.modelM.let { localAABB.transformedBy(it.getRef()) }
                childNodes.add(BVHLeaf(localAABB, mesh))
            }

            // Recursively build child BVH groups
            for (node in skeletonNode.childIds) {
                val child = buildBVHForModel(model, node)
                // Its possible that a model imported from assimp has empty nodes that do not have children. these
                // can be safely ignored since they wouldn't have a bounding box
                if (child != null) {
                    childNodes.add(child)
                }
            }

            if (childNodes.isEmpty()) {
                return null
            }

            // assume that all children nodes will be wrapped in a group node. This could be optimized better
            // later by swapping out the group node for a leaf node. The matrix would have to be set in the leaf
            val aabb = childNodes.map {
                it.getLocalAABB()
            }.reduce(::surroundingBox)
            return BVHGroup(
                aabb,
                childNodes,
                model.skeletonAnimator.nodeTransforms.getValue(currentNodeId).localTransform
            )
        }
    }
}

sealed class BVHNode {
    abstract var aabb: AABB
    abstract fun recomputeAABBIfNeeded(): AABB
    abstract fun isDirty(): Boolean
    abstract fun markClean()
    abstract fun getLocalAABB(): AABB
}

data class BVHLeaf(
    override var aabb: AABB,
    val geometry: Bounded,
    val modelMatrix: TrackedMatrix? = null
) : BVHNode() {
    override fun recomputeAABBIfNeeded(): AABB {
        if (isDirty()) {
            return modelMatrix?.let {aabb.transformedBy(it.get())} ?: aabb
            //aabb = geometry.computeAABB()
            //aabb = modelMatrix?.let { localAABB.transformedBy(it.get().invert()) } ?: aabb
        }
        return aabb
    }

    override fun isDirty(): Boolean {
        return modelMatrix?.isDirty == true
    }

    override fun markClean() {
        modelMatrix?.markClean()
    }

    override fun getLocalAABB(): AABB {
        return modelMatrix?.let {aabb.transformedBy(it.get())} ?: aabb
    }
}

data class BVHInternal(
    override var aabb: AABB,
    val left: BVHNode,
    val right: BVHNode
) : BVHNode() {
    override fun recomputeAABBIfNeeded(): AABB {
        if (isDirty()) {
            val leftAABB = left.recomputeAABBIfNeeded()
            val rightAABB = right.recomputeAABBIfNeeded()
            aabb = surroundingBox(leftAABB, rightAABB)
        }
        return aabb
    }

    override fun isDirty(): Boolean {
        return left.isDirty() || right.isDirty()
    }

    override fun markClean() {
        left.markClean()
        right.markClean()
    }

    override fun getLocalAABB(): AABB {
        return aabb
    }
}

data class BVHGroup(
    override var aabb: AABB,
    val children: List<BVHNode>,
    val modelMatrix: TrackedMatrix? = null
) : BVHNode() {

    override fun recomputeAABBIfNeeded(): AABB {
        aabb = if (isDirty()) {
            val localAABBs = children.map { it.recomputeAABBIfNeeded() }
            localAABBs.reduce(::surroundingBox)
            //modelMatrix?.let { combinedAABB.transformedBy(modelMatrix.getRef()) } ?: aabb
        } else {
            aabb
        }
        return modelMatrix?.let {aabb.transformedBy(it.getRef())} ?: aabb
    }

    override fun isDirty(): Boolean {
        return modelMatrix?.isDirty == true || children.any { it.isDirty() }
    }

    override fun markClean() {
        children.map { it.markClean() }
        modelMatrix?.markClean()
    }

    override fun getLocalAABB(): AABB {
        return modelMatrix?.let {aabb.transformedBy(it.getRef())} ?: aabb
    }
}
