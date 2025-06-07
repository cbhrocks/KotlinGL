package org.kotlingl.model

import org.joml.Vector2fc
import org.joml.Vector3f
import org.kotlingl.entity.Material
import org.kotlingl.shapes.Triangle

data class Vertex (
    val position: Vector3f,
    val normal: Vector3f,
    val uv: Vector2fc,
)

class Mesh(
    val vertices: List<Vertex>,
    val indices: List<Int>,
    val material: Material
) {

    init {
        for (i in indices) {
            require(i in 0..vertices.size)
        }
    }

    fun getTriangles(): List<Triangle> {
        return indices.chunked(3).map({ (i0, i1, i2) ->
            Triangle(
                vertices[i0],
                vertices[i1],
                vertices[i2],
                material
            )
        })
    }
}