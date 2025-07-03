package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.kotlingl.entity.Intersection
import org.kotlingl.shapes.Ray

import org.joml.Vector3f
import org.kotlingl.math.TrackedMatrix
import org.kotlingl.shapes.Bounded
import org.kotlingl.shapes.Intersectable
import org.kotlingl.shapes.Updatable

/**
 * @property modelMatrix Model matrix transforms vertices from model space into parent's space. If there is no parent then into World space.
 * @property modelMInverse Model matrix inverse transforms vertices from the world space into model space
 * @property position The models position in world/parent space
 * @property rotation The models rotation in world/parent space
 * @property scale The models scale in world/parent space
 * @property bvhTree The BVH tree that encompasses this model and all it's children. This tree is created lazily on
 * intersects first call
 */
class Model(
    val name: String,
    val meshes: List<Bounded>,
    val skeleton: Skeleton,
    val nodeToMeshIndices: MutableMap<String, List<Int>> = mutableMapOf(),
    modelMatrix: Matrix4f = Matrix4f()
): Intersectable, Updatable {
    var modelMatrix: Matrix4f = modelMatrix
        private set
    var sharedMatrix = TrackedMatrix(modelMatrix)
        private set
    var modelMInverse: Matrix4f = modelMatrix.invert(Matrix4f())
        private set
    val position: Vector3f
        get() = this@Model.sharedMatrix.getRef().getTranslation(Vector3f())
    val rotation: Quaternionf
        get() = this@Model.sharedMatrix.getRef().getNormalizedRotation(Quaternionf())
    val scale: Vector3f
        get() = this@Model.sharedMatrix.getRef().getScale(Vector3f())
    val bvhTree: BVHTree by lazy {
        BVHTree.buildForModel(this)
    }
    val skeletonAnimator: SkeletonAnimator = SkeletonAnimator(skeleton)

    fun transform(
        modelMatrix: Matrix4f
    ) {
        this.sharedMatrix.set(modelMatrix)
        this.sharedMatrix.getRef().invert(this.modelMInverse)
    }

    fun transform(
        position: Vector3f = this.position,
        rotation: Quaternionf = this.rotation,
        scale: Vector3f = this.scale
    ) {
        this.sharedMatrix.mutate {
            translationRotateScale(
                position,
                rotation,
                scale
            )
            invert(modelMInverse)
        }
    }

    override fun update(timeDelta: Float) {
        skeletonAnimator.update(timeDelta)
        bvhTree.refit()
    }

    override fun intersects(ray: Ray): Intersection? {
        val localRay = ray.transformedBy(modelMInverse)
        val localHit = this.bvhTree.intersects(localRay)
        return localHit?.transformedBy(modelMatrix)
    }
}

class SkeletonAnimator(val skeleton: Skeleton) {
    var currentAnimation: Animation? = null
    var currentTime: Float = 0f

    fun update(deltaTime: Float) {
        val animation = currentAnimation ?: return

        // Advance time
        currentTime += deltaTime * animation.ticksPerSecond
        val timeInTicks = currentTime % animation.duration

        // Apply animation
        applyAnimationToSkeleton(skeleton, animation, timeInTicks)
    }

    fun applyAnimationToSkeleton(skeleton: Skeleton, animation: Animation, time: Float) {
        for ((boneName, nodeAnim) in animation.nodeAnimations) {
            val boneNode = skeleton.nodeMap[boneName] ?: continue

            val position = interpolateKeyframes(time, nodeAnim.positionKeys)
            val rotation = interpolateKeyframes(time, nodeAnim.rotationKeys)
            val scale = interpolateKeyframes(time, nodeAnim.scaleKeys)

            boneNode.localTransform.mutate {
                translationRotateScale(position, rotation, scale)
            }
        }

        // After setting local transforms, propagate global transforms
        updateGlobalTransforms(skeleton.root, Matrix4f())
    }

    fun <T> interpolateKeyframes(time: Float, keys: List<Keyframe<T>>): T {
        if (keys.isEmpty()) throw IllegalArgumentException("No keyframes")
        if (keys.size == 1) return keys[0].value

        // Find two surrounding keyframes
        var i = 0
        while (i < keys.size - 1 && time > keys[i + 1].time) i++

        val key0 = keys[i]
        val key1 = keys[i + 1]
        val t = ((time - key0.time) / (key1.time - key0.time)).coerceIn(0f, 1f)

        return when (val v0 = key0.value) {
            is Vector3f -> Vector3f(v0).lerp(key1.value as Vector3f, t)
            is Quaternionf -> Quaternionf(v0).slerp(key1.value as Quaternionf, t)
            else -> error("Unsupported keyframe type")
        } as T
    }

    fun updateGlobalTransforms(node: SkeletonNode, parentTransform: Matrix4f = Matrix4f()) {
        node.globalTransform = Matrix4f(parentTransform).mul(node.localTransform.getRef())
        if (node.isBone) {
            node.finalTransform = Matrix4f(node.globalTransform).mul(skeleton.inverseBindPoseMap.getValue(node.name))
        }
        node.children.forEach { updateGlobalTransforms(it, node.globalTransform) }
    }
}