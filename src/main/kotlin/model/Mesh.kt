package org.kotlingl.model

import org.joml.Vector2fc
import org.joml.Vector3f
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.Bounded
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Triangle

data class Vertex (
    val position: Vector3f,
    val normal: Vector3f,
    val uv: Vector2fc,
    // Max 4 bones per vertex
    val boneIndices: List<Int> = listOf(),
    val boneWeights: List<Float> = listOf()
)

class Mesh(
    val vertices: List<Vertex>,
    val indices: List<Int>,
    val material: Material
): Bounded {
    val bvhNode: BVHNode by lazy {
        BVHNode.fromBounded(this.triangles)
    }

    val triangles: List<Triangle> by lazy {
        indices.chunked(3).map({ (i0, i1, i2) ->
            Triangle(
                vertices[i0],
                vertices[i1],
                vertices[i2],
                material
            )
        })
    }

    init {
        for (i in indices) {
            require(i in 0..vertices.size)
        }
    }

    override fun intersects(ray: Ray): Intersection? {
        return this.bvhNode.intersects(ray)
    }

    override fun computeAABB(): AABB {
        val min = Vector3f(Float.POSITIVE_INFINITY)
        val max = Vector3f(Float.NEGATIVE_INFINITY)

        for (v in vertices) {
            min.min(v.position)
            max.max(v.position)
        }

        return AABB(min, max)
    }

    override fun getBVHNode(): BVHNode {
        return this.bvhNode
    }

    override fun centroid(): Vector3f {
        val center = Vector3f()
        for (tri in triangles) {
            center.add(tri.centroid())
        }
        return center.div(triangles.size.toFloat())
    }

}