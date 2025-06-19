package model

import org.joml.Vector2f
import org.joml.Vector3f
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.model.BVHNode
import org.kotlingl.model.Mesh
import org.kotlingl.model.Model
import org.kotlingl.model.Vertex
import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.Bounded
import org.kotlingl.shapes.Ray

val cubePositions = listOf(
    Vector3f(-0.5f,-0.5f,-0.5f), // front bottom left 0
    Vector3f(0.5f,-0.5f,-0.5f), // front bottom right 1
    Vector3f(0.5f,0.5f,-0.5f), // front top right 2
    Vector3f(-0.5f,0.5f,-0.5f), // front top left 3
    Vector3f(-0.5f,-0.5f,0.5f), // back bottom left 4
    Vector3f(0.5f,-0.5f,0.5f), // back bottom right 5
    Vector3f(0.5f,0.5f,0.5f), // back top right 6
    Vector3f(-0.5f,0.5f,0.5f), // back top left 7
)

val cubeNormals = listOf(
    Vector3f( 0f,  0f,  1f), // front
    Vector3f( 1f,  0f,  0f), // right
    Vector3f( 0f,  0f, -1f), // back
    Vector3f(-1f,  0f,  0f), // left
    Vector3f( 0f,  1f,  0f), // top
    Vector3f( 0f, -1f,  0f)  // bottom
)

val faceNormals = listOf(
    cubeNormals[0], cubeNormals[0], cubeNormals[0], cubeNormals[0],
    cubeNormals[1], cubeNormals[1], cubeNormals[1], cubeNormals[1],
    cubeNormals[2], cubeNormals[2], cubeNormals[2], cubeNormals[2],
    cubeNormals[3], cubeNormals[3], cubeNormals[3], cubeNormals[3],
    cubeNormals[4], cubeNormals[4], cubeNormals[4], cubeNormals[4],
    cubeNormals[5], cubeNormals[5], cubeNormals[5], cubeNormals[5],
)

val cubeIndices = listOf(
    0,1,2,0,2,3, // front face
    1,5,6,1,6,2, // right face
    5,4,7,5,7,6, // back face
    4,0,3,4,3,7, // left face
    3,2,6,3,6,7, // top face
    4,5,1,4,1,0, // bottom face
)

val cubeVertices = cubeIndices.mapIndexed { index, i ->
    Vertex(
        cubePositions[i],
        faceNormals[i],
        Vector2f()
    )
}

class TestCubeFactory {
    fun createCubeMesh(): Mesh {
        val material = Material()
        return Mesh(
            cubeVertices,
            cubeIndices,
            material
        )
    }

    fun createCubeModel(): Model {
        val material = Material()
        val mesh = Mesh(
            cubeVertices,
            cubeIndices,
            material
        )
        return Model(name = "cube", meshes = listOf(mesh), children = mutableListOf())
    }
}

fun createIntersectingMesh(): Bounded {
    val intersectingMesh = object : Bounded {
        val mat = Material()
        override fun intersects(ray: Ray): Intersection? {
            return Intersection(
                Vector3f(0f, 0f, 0f),
                Vector3f(1f, 0f, 0f),
                5f,
                mat,
                true
            )
        }

        override fun getBVHNode(): BVHNode {
            TODO("Not yet implemented")
        }

        override fun computeAABB(): AABB {
            return AABB(
                Vector3f(),
                Vector3f()
            )
        }

        override fun centroid(): Vector3f {
            TODO("Not yet implemented")
        }
    }
    return intersectingMesh
}
