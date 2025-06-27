package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.kotlingl.entity.Intersection
import org.kotlingl.shapes.Ray

import org.joml.Vector3f
import org.kotlingl.math.TrackedMatrix
import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.Bounded
import org.kotlingl.shapes.Intersectable

/**
 * @property modelM Model matrix transforms vertices from the model space into world space
 * @property modelMInverse Model matrix inverse transforms vertices from the world space into model space
 * @property position The models position in world/parent space
 * @property rotation The models rotation in world/parent space
 * @property scale The models scale in world/parent space
 * @property bvhTree The BVH tree that encompases this model and all it's children. This tree is created lazily on
 * intersects first call
 */
class Model(
    val name: String,
    val meshes: List<Bounded>,
    val skeleton: Skeleton? = null,
    var children: MutableList<Model> = mutableListOf(),
    modelM: Matrix4f = Matrix4f()
): Intersectable {
    var modelM = TrackedMatrix(modelM)
        private set
    var modelMInverse: Matrix4f = modelM.invert(Matrix4f())
        private set
    val position: Vector3f
        get() = modelM.getRef().getTranslation(Vector3f())
    val rotation: Quaternionf
        get() = modelM.getRef().getNormalizedRotation(Quaternionf())
    val scale: Vector3f
        get() = modelM.getRef().getScale(Vector3f())

    val bvhTree: BVHTree by lazy {
        BVHTree.buildForModel(this)
    }

    fun transform(
        modelMatrix: Matrix4f
    ) {
        this.modelM.set(modelMatrix)
        this.modelM.getRef().invert(this.modelMInverse)
    }

    fun transform(
        position: Vector3f = this.position,
        rotation: Quaternionf = this.rotation,
        scale: Vector3f = this.scale
    ) {
        this.modelM.mutate {
            translationRotateScale(
                position,
                rotation,
                scale
            )
            invert(modelMInverse)
        }
    }

    //fun update(parentMatrix: Matrix4fc? = null) {
    //    // Compute world matrix
    //    val worldMatrix = parentMatrix?.mul(modelM.getRef(), Matrix4f()) ?: modelM.getRef()
    //    this.worldMatrix = worldMatrix

    //    // Update own BVH with current transform
    //    bvhTree.refit(worldMatrix)

    //    // Recurse into children
    //    for (child in children) {
    //        child.update(worldMatrix)
    //    }
    //}

    fun addChild(model: Model) {
        this.children.add(model)
    }

    override fun intersects(ray: Ray): Intersection? {
        return this.bvhTree.intersects(ray)
    }

    fun allMeshes(): List<Bounded> {
        return meshes + children.flatMap { it.allMeshes() }
    }

    /* for when rasterization is implemented
    fun Model.draw(shader: Shader) {
        shader.setMatrix("model", transform)
        for (mesh in meshes) {
            mesh.draw(shader)
        }
        children.forEach { it.draw(shader) }
    }
    */
}

