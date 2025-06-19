package model

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kotlingl.entity.Material
import org.kotlingl.model.Mesh
import org.kotlingl.model.Vertex
import org.kotlingl.shapes.Triangle
import kotlin.test.assertIs

class MeshTest {
    lateinit var material: Material
    lateinit var vertices: List<Vertex>
    lateinit var indices: List<Int>
    lateinit var mesh: Mesh

    @BeforeEach
    fun setup() {
        material = Material()
        vertices = listOf(
            Vertex(
                Vector3f(0f, 0f, 0f),
                Vector3f(0f, 0f, -1f),
                Vector2f(0f,0f)
            ),
            Vertex(
                Vector3f(1f, 0f, 0f),
                Vector3f(0f, 0f, -1f),
                Vector2f(0f,0f)
            ),
            Vertex(
                Vector3f(1f, 1f, 0f),
                Vector3f(0f, 0f, -1f),
                Vector2f(0f,0f)
            ),
            Vertex(
                Vector3f(0f, 1f, 0f),
                Vector3f(0f, 0f, -1f),
                Vector2f(0f,0f)
            ),
        )
        indices = listOf(
            0, 1, 2, 0, 2, 3
        )
        mesh = Mesh(
            vertices,
            indices,
            material
        )
    }

    @Test
    fun `creating a mesh with less than 3 vertices fails`() {
        assertThrows<IllegalArgumentException> {
            Mesh(listOf(), listOf(), material)
        }
    }

    @Test
    fun `triangles converts vertices and indices to triangles`() {
        val triangles = mesh.triangles

        assertEquals(2, triangles.size)
        assertEquals(triangles[0].v0, vertices[0])
        assertEquals(triangles[0].v1, vertices[1])
        assertEquals(triangles[0].v2, vertices[2])
        assertEquals(triangles[1].v0, vertices[0])
        assertEquals(triangles[1].v1, vertices[2])
        assertEquals(triangles[1].v2, vertices[3])
    }

    @Test
    fun `computeAABB encapsulates all vertices`() {
        val aabb = mesh.computeAABB()
        assertEquals(aabb.min, Vector3f(0f, 0f, 0f))
        assertEquals(aabb.max, Vector3f(1f, 1f, 0f))
    }

    @Test
    fun `bvhNode creates a tree with one leaf per triangle`() {
        val bvhNode = mesh.bvhNode
        assertNull(bvhNode.left?.left)
        assertNull(bvhNode.left?.right)
        assertIs<Triangle>(bvhNode.left?.leaf)
        assertNull(bvhNode.right?.left)
        assertNull(bvhNode.right?.right)
        assertIs<Triangle>(bvhNode.right?.leaf)
    }

    @Test
    fun `centroid is in the middle of the mesh`() {
        assertEquals(mesh.centroid(), Vector3f(0.5f, 0.5f, 0f))
    }
}