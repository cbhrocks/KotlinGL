package org.kotlingl.model

import org.kotlingl.math.Matrix4

data class Bone (
    val name: String,
    val offsetMatrix: Matrix4,               // Inverse bind pose
    var transformMatrix: Matrix4 = Matrix4.identity(),  // Local animation transform
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