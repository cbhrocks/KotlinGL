package org.kotlingl.model

import org.kotlingl.entity.Material
import org.kotlingl.math.Vector2
import org.kotlingl.math.Vector3
import org.kotlingl.shapes.Triangle

data class Vertex (
    val position: Vector3,
    val normal: Vector3,
    val uv: Vector2,
)

class Mesh(
    val vertices: List<Vertex>,
    val indices: List<Int>,
    val materialIndex: Int
) {

    init {
        for (i in indices) {
            require(i in 0..vertices.size)
        }
    }

    fun getTriangles(material: Material): List<Triangle> {
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