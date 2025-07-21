package org.kotlingl.model

import org.joml.Quaternionf
import org.joml.Vector2f
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

data class SpriteBB (
    val min: Vector2f,
    val max: Vector2f,
)

data class Animation2D (
    val name: String,
    val duration: Float,
    val ticksPerSecond: Float,
    val keyframes: List<Keyframe<SpriteBB>>,
)
