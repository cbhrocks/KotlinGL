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
 * the static immutable data describing a skeleton. This should be only created once each time a model is loaded.
 *
 * @property name the name attached to each aiNode object loaded in with ASSIMP. This name is used to link mesh bones
 * to the node.
 * @property parentName the name of the parent aiNode. This is used to establish the node hierarchy
 * @property localTransform the local bind transformation from parent node to this node.
 * @property childNames a list of child aiNode names.
 * @property isBone whether this node is used as a bone by a mesh.
 */
data class SkeletonNode(
    val id: Int,
    val name: String,
    val localTransform: Matrix4f,
    val childNames: List<String> = listOf(),
    val isBone: Boolean,
    val parentName: String? = null,
)

/**
 * A joint in the skeleton
 *
 * @property name used to link the BoneNode object various bones that use the animation transforms.
 */
data class SkeletonNodeTransforms(
    val id: Int,
    val name: String,
    var localTransform: TrackedMatrix, // Local transform (T * R * S)
    var globalTransform: Matrix4f = Matrix4f(), // Computed during animation
    var finalTransform: Matrix4f? = null, // only used if isBone is true. computed by globalTransform * inverseBindPose
)

data class Skeleton(
    val name: String,
    val rootId: Int,
    val nodeMap: Map<Int, SkeletonNode>,
    val inverseBindPoseMap: Map<Int, Matrix4f> = mapOf(),
    val animations: Map<String, Animation> = mapOf(),
)