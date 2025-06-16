package model
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
import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.Bounded
import org.kotlingl.shapes.Ray

class ModelTest {

    lateinit var mat: Material
    lateinit var mesh: Mesh
    lateinit var childMesh: Mesh
    lateinit var ray: Ray

    @BeforeEach
    fun setup() {
        mat = Material()
        mesh = Mesh(listOf(), listOf(), mat)
        childMesh = Mesh(listOf(), listOf(), mat)
        ray = Ray(origin = Vector3f(0f, 0f, 0f), direction = Vector3f(1f, 0f, 0f))
    }

    @Test
    fun `allMeshes returns all meshes from self and children`() {
        val childModel = Model(name = "child", meshes = listOf(childMesh), children = mutableListOf())
        val parentModel = Model(name = "parent", meshes = listOf(mesh), children = mutableListOf(childModel))

        val result = parentModel.allMeshes()

        assertEquals(2, result.size)
        assertTrue(result.contains(mesh))
        assertTrue(result.contains(childMesh))
    }

    @Test
    fun `intersects returns null if no mesh intersects`() {
        val model = Model(name = "model", meshes = listOf(mesh), children = mutableListOf())
        val result = model.intersects(ray)

        assertNull(result)
    }

    @Test
    fun `intersects returns result from intersecting mesh`() {
        // Fake mesh that will return an intersection
        val intersectingMesh = object : Bounded {
            override fun intersects(ray: Ray): Intersection? {
                return Intersection(Vector3f(0f, 0f, 0f), Vector3f(1f, 0f, 0f), 5f, )
            }

            override fun getBVHNode(): BVHNode {
                TODO("Not yet implemented")
            }

            override fun computeAABB(): AABB {
                TODO("Not yet implemented")
            }

            override fun centroid(): Vector3f {
                TODO("Not yet implemented")
            }
        }

        val model = Model(name = "model", meshes = listOf(intersectingMesh), children = listOf())
        val result = model.intersects(ray)

        assertNotNull(result)
        assertEquals(1.0f, result?.distance)
    }

    @Test
    fun `model can be constructed with skeleton`() {
        val boneNode = BoneNode(name = "root", localTransform = Matrix4.identity(), globalTransform = Matrix4.identity())
        val model = Model(name = "skelModel", meshes = listOf(), children = listOf(), skeleton = boneNode)

        assertEquals("root", model.skeleton?.name)
    }
}
