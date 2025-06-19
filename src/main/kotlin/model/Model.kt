package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Triangle
import org.lwjgl.assimp.AIColor4D
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMesh

import org.joml.Vector3f
import org.kotlingl.entity.Texture
import org.kotlingl.entity.toColor
import org.kotlingl.math.toJoml
import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.Bounded
import org.lwjgl.assimp.AIBone
import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.AIString
import org.lwjgl.assimp.Assimp.*
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.toPath

class Model(
    val name: String,
    val meshes: List<Bounded>,
    val skeleton: BoneNode? = null,
    var children: MutableList<Model> = mutableListOf()
) : Bounded {
    private var modelM: Matrix4f = Matrix4f()
    private var modelMInverse: Matrix4f = Matrix4f()
    private var position: Vector3f = Vector3f(0f,0f,0f)
    private var rotation: Quaternionf = Quaternionf()
    private var scale: Vector3f = Vector3f(1f, 1f, 1f)

    val bvhNode: BVHNode by lazy {
        BVHNode.fromBounded(listOf(this.meshes, this.children).flatten())
    }

    fun transform(
        mat: Matrix4f
    ) {
        this.modelM = mat
        mat.invert(this.modelMInverse)
    }

    fun transform(
        position: Vector3f = this.position,
        rotation: Quaternionf = this.rotation,
        scale: Vector3f = this.scale
    ) {
        this.modelM = this.modelM.identity().translation(position)
            .rotation(rotation)
            .scale(scale)
        this.modelM.invert(this.modelMInverse)
    }

    init {
        transform()
    }

    fun addChild(model: Model) {
        this.children.add(model)
    }

    override fun intersects(ray: Ray): Intersection? {
        val localRay = ray.transformedBy(modelMInverse)

        //val localHit = (meshes.mapNotNull {
        //    it.intersects(ray)
        //} + children.mapNotNull { it.intersects(localRay) })
        //    .minByOrNull { it.t }
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

