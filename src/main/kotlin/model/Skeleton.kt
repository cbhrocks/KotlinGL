package org.kotlingl.model

import org.joml.Matrix4f
import org.kotlingl.math.TrackedMatrix

data class VertexWeight(
    val vertexId: Int,
    val weight: Float
)

/**
 * data describing how a joint (BoneNode) deforms specific vertices in a mesh
 *
 * @property name used to link the Bone object to the BoneNode stored at the Mesh level
 */
data class Bone (
    val name: String,
    val offsetMatrix: Matrix4f, // Inverse bind pose. tranforms from mesh space -> bone space
    val weights: List<VertexWeight>
)

/**
 * A joint in the skeleton
 *
 * @property name used to link the BoneNode object various bones that use the animation transforms.
 */
data class SkeletonNode(
    val name: String,
    var localTransform: TrackedMatrix, // Local transform (T * R * S)
    var globalTransform: Matrix4f = Matrix4f(), // Computed during animation
    var finalTransform: Matrix4f? = null, // only used if isBone is true. computed by globalTransform * inverseBindPose
    var isBone: Boolean = false,
    val children: List<SkeletonNode> = listOf(),
    var parent: SkeletonNode? = null,
) {
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + localTransform.getRef().hashCode()
        for (child in children) {
            result = 31 * result + child.hashCode()
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SkeletonNode

        if (name != other.name) return false
        if (localTransform != other.localTransform) return false
        if (globalTransform != other.globalTransform) return false
        if (children != other.children) return false
        if (parent != other.parent) return false

        return true
    }

    fun deepCopy(): SkeletonNode {
        val copied = SkeletonNode(
            this.name,
            localTransform = TrackedMatrix(this.localTransform.get()),
            globalTransform = Matrix4f(this.globalTransform),
            isBone = this.isBone,
            children = this.children.map { it.deepCopy() },
        )
        copied.children.forEach { it.parent = copied }
        return copied
    }

    fun flatten(): List<SkeletonNode> {
        return listOf(this) + children.flatMap { it.flatten() }
    }
}

data class Skeleton(
    val name: String,
    val root: SkeletonNode,
    val nodeMap: Map<String, SkeletonNode>,
    val animations: Map<String, Animation>,
    val inverseBindPoseMap: Map<String, Matrix4f>
) {
    override fun hashCode(): Int = root.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Skeleton

        if (name != other.name) return false
        if (root != other.root) return false
        if (nodeMap != other.nodeMap) return false
        if (animations != other.animations) return false

        return true
    }

    fun deepCopy(): Skeleton {
        val newRoot = this.root.deepCopy()

        return Skeleton(
            this.name,
            newRoot,
            newRoot.flatten().associateBy { it.name },
            this.animations,
            this.inverseBindPoseMap
        )
    }
}