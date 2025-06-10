package org.kotlingl.model

import org.joml.Matrix4f

data class VertexWeight(
    val vertexId: Int,
    val weight: Float
)

data class Bone (
    val name: String,
    val offsetMatrix: Matrix4f, // Inverse bind pose. tranforms from mesh space -> bone space
    val weights: List<VertexWeight>
)

data class BoneNode(
    val name: String,
    var localTransform: Matrix4f, // Local transform (T * R * S)
    var globalTransform: Matrix4f = Matrix4f(), // Computed during animation
    val children: MutableList<BoneNode> = mutableListOf(),
    var parent: BoneNode? = null
)