package shapes

import org.joml.Matrix4f
import org.joml.Vector3f
import org.kotlingl.shapes.Ray
import kotlin.test.Test
import kotlin.test.assertEquals

class RayTest {

    @Test
    fun `transformedBy updates vectors`() {
        val ray = Ray(Vector3f(0f, 0f, 0f), Vector3f(0f, 0f, -1f))
        val newRay = ray.transformedBy(Matrix4f().translation(1f, 1f, 1f))
        assertEquals(Vector3f(1f, 1f, 1f), newRay.origin)
        assertEquals(Vector3f(0f, 0f, -1f), newRay.direction)
    }
}