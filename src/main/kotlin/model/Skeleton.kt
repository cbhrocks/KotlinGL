package org.kotlingl.model

import org.joml.Matrix4f

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
data class BoneNode(
    val name: String,
    var localTransform: Matrix4f, // Local transform (T * R * S)
    var globalTransform: Matrix4f = Matrix4f(), // Computed during animation
    val children: List<BoneNode> = listOf(),
    var parent: BoneNode? = null,
) {
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + localTransform.hashCode()
        for (child in children) {
            result = 31 * result + child.hashCode()
        }
        return result
    }
}