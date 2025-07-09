package org.kotlingl.model

import org.joml.Quaternionf
import org.joml.Vector3f

data class Animation(
    val name: String,
    val duration: Float,
    val ticksPerSecond: Float,
    val nodeAnimations: Map<String, NodeAnimation>,
    val isLoop: Boolean = true
)

data class Keyframe<T>(val time: Float, val value: T)

data class NodeAnimation(
    val nodeName: String,
    val positionKeys: List<Keyframe<Vector3f>>,
    val rotationKeys: List<Keyframe<Quaternionf>>,
    val scaleKeys: List<Keyframe<Vector3f>>,
)
