package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.kotlingl.entity.Intersection
import org.kotlingl.shapes.Ray

import org.joml.Vector3f
import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.Bounded

/**
 * @property modelM Model matrix transforms vertices from the model space into world space
 * @property modelMInverse Model matrix inverse transforms vertices from the world space into model space
 * @property position The models position in world/parent space
 * @property rotation The models rotation in world/parent space
 * @property scale The models scale in world/parent space
 * @property bvhNode The root node of the bvh tree that contains one child or mesh per node
 */
class Model(
    val name: String,
    val meshes: List<Bounded>,
    val skeleton: BoneNode? = null,
    var children: MutableList<Model> = mutableListOf(),
    modelM: Matrix4f = Matrix4f()
) : Bounded {
    var modelM = Matrix4f()
        private set
    var modelMInverse: Matrix4f = Matrix4f()
        private set
    val position: Vector3f
        get() = modelM.getTranslation(Vector3f())
    val rotation: Quaternionf
        get() = modelM.getNormalizedRotation(Quaternionf())
    val scale: Vector3f
        get() = modelM.getScale(Vector3f())

    val bvhNode: BVHNode by lazy {
        BVHNode.fromBounded(listOf(this.meshes, this.children).flatten())
    }

    init {
        transform(modelM)
    }

    fun transform(
        modelMatrix: Matrix4f
    ) {
        this.modelM = modelMatrix
        this.modelM.invert(this.modelMInverse)
    }

    fun transform(
        position: Vector3f = this.position,
        rotation: Quaternionf = this.rotation,
        scale: Vector3f = this.scale
    ) {
        this.modelM.translationRotateScale(
            position,
            rotation,
            scale
        )
        this.modelM.invert(this.modelMInverse)
    }

    fun addChild(model: Model) {
        this.children.add(model)
    }

    override fun intersects(ray: Ray): Intersection? {
        val localRay = ray.transformedBy(modelMInverse)

        val localHit = this.bvhNode.intersects(localRay)

        return localHit?.let {
            val hitPointWorld = modelM.transformPosition(localHit.point, Vector3f())
            val hitWorldNormal = modelM.transformDirection(localHit.normal, Vector3f()).normalize()
            val frontFace = ray.direction.dot(hitWorldNormal) < 0

            Intersection(
                hitPointWorld,
                hitWorldNormal,
                localHit.t,
                localHit.material,
                frontFace,
                localHit.uv
            )
        }
    }

    override fun getBVHNode(): BVHNode {
        return this.bvhNode
    }

    override fun computeAABB(): AABB {
        return this.bvhNode.boundingBox
        //return this.meshes.map { computeAABB() }.reduce { acc, curAABB -> AABB.surroundingBox(acc, curAABB) }
    }

    override fun centroid(): Vector3f {
        val center = Vector3f()
        val allBounded = meshes + children
        for (bounded in allBounded) {
            center.add(bounded.centroid())
        }
        return center.div(allBounded.size.toFloat())
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

