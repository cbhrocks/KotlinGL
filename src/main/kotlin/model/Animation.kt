package org.kotlingl.model

import org.joml.Quaternionf
import org.joml.Vector3f

data class Animation(
    val name: String,
    val duration: Float,
    val ticksPerSecond: Float,
    val nodeAnimations: Map<Int, NodeAnimation>,
    var isLoop: Boolean = true
)

data class Keyframe<T>(val time: Float, val value: T)

data class NodeAnimation(
    val nodeId: Int,
    val positionKeys: List<Keyframe<Vector3f>>,
    val rotationKeys: List<Keyframe<Quaternionf>>,
    val scaleKeys: List<Keyframe<Vector3f>>,
)
