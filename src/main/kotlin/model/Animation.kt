package org.kotlingl.model

import org.joml.Quaternionf
import org.joml.Vector3f

data class Animation(
    val name: String,
    val duration: Double,
    val ticksPerSecond: Double,
    val nodeAnimations: Map<String, NodeAnimation>
)

data class Keyframe<T>(val time: Double, val value: T)

data class NodeAnimation(
    val nodeName: String,
    val positionKeys: List<Keyframe<Vector3f>>,
    val rotationKeys: List<Keyframe<Quaternionf>>,
    val scaleKeys: List<Keyframe<Vector3f>>
)
