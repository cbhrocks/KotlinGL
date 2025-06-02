package org.kotlingl.shapes

import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.math.Vector2
import org.kotlingl.math.Vector3

data class Vertex (
    val position: Vector3,
    val normal: Vector3,
    val uv: Vector2,
)

class Mesh(
    override val material: Material,
    val vertices: List<Vertex>,
    val indices: List<Int>,
): Shape {

    init {
        for (i in indices) {
            require(i in 0..vertices.size)
        }
    }


    override fun intersects(ray: Ray): Intersection? {
        TODO("Not yet implemented")
    }
}