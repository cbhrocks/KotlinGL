package model
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.model.BVHNode
import org.kotlingl.model.BoneNode
import org.kotlingl.model.Mesh
import org.kotlingl.model.Model
import org.kotlingl.model.Vertex
import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.Bounded
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Triangle
import kotlin.test.assertIs

class ModelTest {

    lateinit var mat: Material
    lateinit var mesh: Mesh
    lateinit var childMesh: Mesh
    lateinit var childModel: Model
    lateinit var parentModel: Model

    @BeforeEach
    fun setup() {
        mat = Material()
        mesh = Mesh(listOf(
            Vertex(
                Vector3f(0f,0f,0f),
                Vector3f(0f, 0f, -1f),
                Vector2f()
            ),
            Vertex(
                Vector3f(1f,0f,0f),
                Vector3f(0f, 0f, -1f),
                Vector2f()
            ),
            Vertex(
                Vector3f(0f,1f,0f),
                Vector3f(0f, 0f, -1f),
                Vector2f()
            )
        ), listOf(0,1,2), mat)
        childMesh = Mesh(listOf(
            Vertex(
                Vector3f(1f,0f,0f),
                Vector3f(0f, 0f, -1f),
                Vector2f()
            ),
            Vertex(
                Vector3f(1f,1f,0f),
                Vector3f(0f, 0f, -1f),
                Vector2f()
            ),
            Vertex(
                Vector3f(0f,1f,0f),
                Vector3f(0f, 0f, -1f),
                Vector2f()
            )
        ), listOf(0,1,2), mat)
        childModel = Model(name = "child", meshes = listOf(childMesh), children = mutableListOf())
        parentModel = Model(name = "parent", meshes = listOf(mesh), children = mutableListOf(childModel))
    }

    @Test
    fun `allMeshes returns all meshes from self and children`() {
        val result = parentModel.allMeshes()

        assertEquals(2, result.size)
        assertTrue(result.contains(mesh))
        assertTrue(result.contains(childMesh))
    }

    @Test
    fun `bvhNode creates a tree with one leaf per mesh and child`() {
        val bvhNode = parentModel.bvhNode
        assertNull(bvhNode.left?.left)
        assertNull(bvhNode.left?.right)
        assertIs<Mesh>(bvhNode.left?.leaf)
        assertNull(bvhNode.right?.left)
        assertNull(bvhNode.right?.right)
        assertIs<Model>(bvhNode.right?.leaf)
    }

    @Test
    fun `computeAABB encapsulates all meshes and children`() {
        val aabb = mesh.computeAABB()
        assertEquals(aabb.min, Vector3f(0f, 0f, 0f))
        assertEquals(aabb.max, Vector3f(1f, 1f, 0f))
    }

    @Test
    fun `centroid is in the middle of the model and its children`() {
        assertEquals(parentModel.centroid(), Vector3f(0.5f, 0.5f, 0f))
    }

    @Test
    fun `intersects returns null if no mesh or child intersects`() {
        val ray = Ray(origin = Vector3f(0f, 0f, 0f), direction = Vector3f(1f, 0f, 0f))
        val result = parentModel.intersects(ray)
        assertNull(result)
    }

    @Test
    fun `intersects returns result from intersecting mesh`() {
        val origin = Vector3f(0f, 0f, -1f)
        val direction = childModel.centroid().sub(origin).normalize()
        val ray = Ray(origin, direction)
        val result = parentModel.intersects(ray)
        assertNotNull(result)
        assertEquals(origin.distance(childModel.centroid()), result?.t)
    }

    @Test
    fun `model can be constructed with skeleton`() {
        val boneNode = BoneNode(name = "root", localTransform = Matrix4f(), globalTransform = Matrix4f())
        val model = Model(name = "skelModel", meshes = listOf(), children = mutableListOf(), skeleton = boneNode)

        assertEquals("root", model.skeleton?.name)
    }
}
