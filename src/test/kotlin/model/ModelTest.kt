package model
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.kotlingl.entity.Material
import org.kotlingl.model.SkeletonNode
import org.kotlingl.model.Mesh
import org.kotlingl.model.Model
import org.kotlingl.model.Vertex
import org.kotlingl.shapes.Ray
import kotlin.math.PI
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
    fun `mesh deep in model tree intersect`() {
        val grandParentModel = Model("grandparent", listOf(), children = mutableListOf(parentModel))
        val origin = Vector3f(0f, 0f, -1f)
        val direction = childModel.centroid().sub(origin).normalize()
        val ray = Ray(origin, direction)
        val result = parentModel.intersects(ray)
        assertNotNull(result)
        assertEquals(origin.distance(childModel.centroid()), result?.t)
    }

    @Test
    fun `transform updates models position rotation and scale`() {
        val oldMatrix = parentModel.sharedMatrix.clone()
        val newPosition = Vector3f(1f, 1f, 1f)
        val newRotation = Quaternionf().rotateY(PI.toFloat())
        val newScale = Vector3f(2f, 2f, 2f)
        parentModel.transform(newPosition, newRotation, newScale )
        assertEquals(parentModel.position, newPosition)
        assertEquals(parentModel.rotation, newRotation)
        assertEquals(parentModel.scale, newScale)
        assertNotEquals(parentModel.sharedMatrix, oldMatrix)
    }

    @Test
    fun `translated model intersects`() {
        parentModel.transform(position = Vector3f(1f, 0f, 0f))
        val missRay = Ray(Vector3f(0.5f, 0.5f, -1f), Vector3f(0f, 0f, 1f))
        val result = parentModel.intersects(missRay)
        assertNull(result)

        val hitRay = Ray(Vector3f(1.5f, 0.5f, -1f), Vector3f(0f, 0f, 1f))
        val hitResult = parentModel.intersects(hitRay)
        assertNotNull(hitResult)
    }

    @Test
    fun `rotated model intersects`() {
        parentModel.transform(rotation = Quaternionf().rotateY(PI.toFloat()))
        val missRay = Ray(Vector3f(0.5f, 0.5f, -1f), Vector3f(0f, 0f, 1f))
        val result = parentModel.intersects(missRay)
        assertNull(result)

        val hitRay = Ray(Vector3f(-0.5f, 0.5f, -1f), Vector3f(0f, 0f, 1f))
        val hitResult = parentModel.intersects(hitRay)
        assertNotNull(hitResult)
    }

    @Test
    fun `scaled model intersects`() {
        parentModel.transform(scale = Vector3f(0.5f, 0.5f, 0.5f))
        val missRay = Ray(Vector3f(0.75f, 0.75f, -1f), Vector3f(0f, 0f, 1f))
        val result = parentModel.intersects(missRay)
        assertNull(result)

        val hitRay = Ray(Vector3f(0.25f, 0.25f, -1f), Vector3f(0f, 0f, 1f))
        val hitResult = parentModel.intersects(hitRay)
        assertNotNull(hitResult)
    }

    @Test
    fun `model can be constructed with skeleton`() {
        val skeletonNode = SkeletonNode(name = "root", localTransform = Matrix4f(), globalTransform = Matrix4f())
        val model = Model(name = "skelModel", meshes = listOf(), children = mutableListOf(), skeleton = skeletonNode)

        assertEquals("root", model.skeleton?.name)
    }
}
