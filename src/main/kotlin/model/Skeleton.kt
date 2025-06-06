package org.kotlingl.model

import org.joml.Matrix4f

data class Bone (
    val name: String,
    val offsetMatrix: Matrix4f,               // Inverse bind pose
    var transformMatrix: Matrix4f = Matrix4f(),  // Local animation transform
    val children: MutableList<Bone> = mutableListOf(),  // Bone hierarchy
    var parent: Bone? = null
)

class Skeleton (
    val rootBone: Bone,
    val boneMap: Map<String, Bone>
) {
    fun update() {

    }
}