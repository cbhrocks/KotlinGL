package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.kotlingl.entity.Material
import org.kotlingl.math.TrackedMatrix

object PrimitiveFactory {
    fun createSphere(
        name: String,
        segments: Int = 32,
        rings: Int = 16,
        position: Vector3f = Vector3f(),
        rotation: Quaternionf = Quaternionf(),
        scale: Vector3f = Vector3f(1f),
        material: Material = Material()
    ): Model {
        val vertices = mutableListOf<Vertex>()
        val indices = mutableListOf<Int>()

        for (y in 0..rings) {
            val v = y.toFloat() / rings
            val theta = v * Math.PI
            val sinTheta = Math.sin(theta)
            val cosTheta = Math.cos(theta)

            for (x in 0..segments) {
                val u = x.toFloat() / segments
                val phi = u * Math.PI * 2
                val sinPhi = Math.sin(phi)
                val cosPhi = Math.cos(phi)

                val px = (cosPhi * sinTheta).toFloat()
                val py = cosTheta.toFloat()
                val pz = (sinPhi * sinTheta).toFloat()

                val positionVec = Vector3f(px, py, pz)
                val normalVec = Vector3f(px, py, pz).normalize()
                val uv = Vector2f(u, 1f - v)

                vertices.add(Vertex(positionVec, normalVec, uv))
            }
        }

        val vertCountX = segments + 1
        for (y in 0 until rings) {
            for (x in 0 until segments) {
                val i0 = y * vertCountX + x
                val i1 = i0 + 1
                val i2 = i0 + vertCountX
                val i3 = i2 + 1

                indices.addAll(listOf(i0, i2, i1))
                indices.addAll(listOf(i1, i2, i3))
            }
        }

        val mesh = Mesh(vertices, indices, material)
        return Model(
            name,
            listOf(mesh),
            Skeleton(
                "unnamedSkeleton",
                0,
                mapOf(
                    0 to SkeletonNode(
                        0,
                        "root",
                        Matrix4f(),
                        isBone = false
                    )
                ),
            ),
            mutableMapOf(0 to listOf(0))
        )
    }
}