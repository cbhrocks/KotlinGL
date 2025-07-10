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
 * @property id a unique numerical id created for each skeleton node when imported with ASSIMP
 * @property name the name attached to each aiNode object loaded in with ASSIMP. This name is used to link mesh bones
 * to the node.
 * @property localTransform the local bind transformation from parent node to this node.
 * @property childIds a list of child SkeletonNode ids.
 * @property isBone whether this node is used as a bone by a mesh.
 * @property parentId the id of the parent SkeletonNode. This is used to establish the node hierarchy
 */
data class SkeletonNode(
    val id: Int,
    val name: String,
    val localTransform: Matrix4f,
    val childIds: List<Int> = listOf(),
    val isBone: Boolean,
    val parentId: Int? = null,
)

/**
 * A joint in the skeleton
 *
 * @property name used to link the BoneNode object various bones that use the animation transforms.
 */
data class SkeletonNodeTransforms(
    val id: Int,
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