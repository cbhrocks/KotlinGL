package model
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.kotlingl.entity.Material
import org.kotlingl.model.SkeletonNodeTransforms
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
    }

    @Test
    fun `allMeshes returns all meshes from self and children`() {
    }

    @Test
    fun `bvhNode creates a tree with one leaf per mesh and child`() {
    }

    @Test
    fun `computeAABB encapsulates all meshes and children`() {
        val aabb = mesh.computeAABB()
        assertEquals(aabb.min, Vector3f(0f, 0f, 0f))
        assertEquals(aabb.max, Vector3f(1f, 1f, 0f))
    }

    @Test
    fun `centroid is in the middle of the model and its children`() {
    }

    @Test
    fun `intersects returns null if no mesh or child intersects`() {
        val ray = Ray(origin = Vector3f(0f, 0f, 0f), direction = Vector3f(1f, 0f, 0f))
        val result = parentModel.intersects(ray)
        assertNull(result)
    }

    @Test
    fun `intersects returns result from intersecting mesh`() {
    }

    @Test
    fun `mesh deep in model tree intersect`() {
    }

    @Test
    fun `transform updates models position rotation and scale`() {
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
    }
}
