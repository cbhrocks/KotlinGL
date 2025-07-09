package org.kotlingl.model

import ShaderProgram
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.kotlingl.entity.Intersection
import org.kotlingl.shapes.Ray

import org.joml.Vector3f
import org.kotlingl.math.TrackedMatrix
import org.kotlingl.shapes.Drawable
import org.kotlingl.shapes.GLResource
import org.kotlingl.shapes.Intersectable
import org.kotlingl.shapes.Updatable
import org.kotlingl.utils.isGLReady

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
    val meshes: List<Mesh>,
    val skeleton: Skeleton,
    val nodeToMeshIndices: MutableMap<Int, List<Int>>,
    modelMatrix: Matrix4f = Matrix4f()
): Intersectable, Updatable, GLResource(), Drawable {
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
    val skeletonAnimator: SkeletonAnimator = SkeletonAnimator(skeleton, sharedMatrix)

    override fun initGL() {
        require(isGLReady()) { "GL has not been initialized" }

        meshes.forEach { it.initGL() }
        markInitialized()
    }

    override fun draw(shader: ShaderProgram) {
        shader.use()

        // Upload model transform and inverse
        //shader.setUniform("uModel", modelMatrix)
        //shader.setUniform("uModelInverse", modelMatrix.invert(Matrix4f()))

        drawMeshByNode(skeleton.rootName, shader)
    }

    private fun drawMeshByNode(nodeName: String, shader: ShaderProgram) {
        val nodeTransforms = skeletonAnimator.nodeTransforms.getValue(nodeName)
        // Upload final matrix for this set of meshes (uniform name depends on shader)
        shader.setUniform(
            "uModel",
            nodeTransforms.finalTransform ?: nodeTransforms.globalTransform
        )
        nodeToMeshIndices.getValue(nodeName).forEach {
            meshes[it].draw(shader)
        }
        skeleton.nodeMap.getValue(nodeName).childNames.forEach {
            drawMeshByNode(it, shader)
        }
    }

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
        return localHit?.transformedBy(sharedMatrix.getRef())
    }
}

class SkeletonAnimator(val skeleton: Skeleton, val modelMatrix: TrackedMatrix) {
    var currentAnimation: Animation? = null
    var currentTime: Float = 0f
    var nodeTransforms: MutableMap<String, SkeletonNodeTransforms> = mutableMapOf()

    init {
        nodeTransforms = skeleton.nodeMap.mapValues {
            SkeletonNodeTransforms(
                it.key,
                TrackedMatrix(Matrix4f(it.value.localTransform))
            )
        }.toMutableMap()
        updateGlobalTransforms(skeleton.rootName, modelMatrix.getRef())
    }

    fun update(deltaTime: Float) {
        currentAnimation?.let {
            // Advance time
            currentTime += deltaTime * it.ticksPerSecond
            val timeInTicks = currentTime % (it.duration)

            // Apply animation
            applyAnimationToSkeleton(skeleton, it, timeInTicks)
        }

        // After setting local transforms, propagate global transforms
        updateGlobalTransforms(skeleton.rootName, modelMatrix.getRef())
    }

    fun applyAnimationToSkeleton(skeleton: Skeleton, animation: Animation, time: Float) {
        for ((boneName, nodeAnim) in animation.nodeAnimations) {
            val position = interpolateKeyframes(time, animation.duration, nodeAnim.positionKeys, animation.isLoop)
            val rotation = interpolateKeyframes(time, animation.duration, nodeAnim.rotationKeys, animation.isLoop)
            val scale = interpolateKeyframes(time, animation.duration, nodeAnim.scaleKeys, animation.isLoop)

            nodeTransforms.getValue(boneName).localTransform.mutate {
                translationRotateScale(position, rotation, scale)
            }
        }
    }

    fun <T> interpolateKeyframes(time: Float, duration: Float, keys: List<Keyframe<T>>, isLoop: Boolean): T {
        if (keys.isEmpty()) throw IllegalArgumentException("No keyframes")
        if (keys.size == 1) return keys[0].value

        val t = if (isLoop) time % duration else time.coerceIn(0f, duration)

        // Find two surrounding keyframes
        var i = 0
        while (i < keys.size - 1 && time > keys[i + 1].time) i++

        // val key0 = keys[i]
        // val key1 = keys[i + 1]
        // val t = ((time - key0.time) / (key1.time - key0.time)).coerceIn(0f, 1f)
        val key0 = keys[i]
        val key1 = if (i + 1 < keys.size) {
            keys[i + 1]
        } else if (isLoop) {
            // Loop back to the first keyframe
            keys[0].copy(time = duration + keys[0].time)
        } else {
            // Clamp to last keyframe in non-looping case
            return keys.last().value
        }
        val localT = ((t - key0.time) / (key1.time - key0.time)).coerceIn(0f, 1f)

        return when (val v0 = key0.value) {
            is Vector3f -> Vector3f(v0).lerp(key1.value as Vector3f, localT)
            is Quaternionf -> Quaternionf(v0).slerp(key1.value as Quaternionf, localT)
            else -> error("Unsupported keyframe type")
        } as T
    }

    fun updateGlobalTransforms(nodeName: String, parentGlobal: Matrix4f = Matrix4f()) {
        val currentNode = skeleton.nodeMap.getValue(nodeName)
        val currentTransforms = nodeTransforms.getValue(nodeName)

        parentGlobal.mul(currentTransforms.localTransform.getRef(), currentTransforms.globalTransform)
        if (currentNode.isBone) {
            parentGlobal.mul(
                skeleton.inverseBindPoseMap.getValue(nodeName),
                currentTransforms.finalTransform ?: Matrix4f()
            )
        }

        currentNode.childNames.forEach {
            updateGlobalTransforms(it, currentTransforms.globalTransform)
        }
    }
}