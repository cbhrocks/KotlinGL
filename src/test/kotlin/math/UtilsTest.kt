package math

import org.joml.Matrix4f
import org.kotlingl.math.EPSILON
import org.kotlingl.math.toJoml
import org.lwjgl.assimp.AIMatrix4x4
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {

    @Test
    fun `toJoml converts matrices with translation properly`() {
        val aiMatrix = AIMatrix4x4.create().apply {
            a1(1f); b1(0f); c1(0f); d1(0f)
            a2(0f); b2(1f); c2(0f); d2(0f)
            a3(0f); b3(0f); c3(1f); d3(0f)
            a4(5f); b4(6f); c4(7f); d4(1f)
        }

        val expected = Matrix4f().translation(5f, 6f, 7f)
        val result = aiMatrix.toJoml()

        assertEquals(expected, result)
    }

    @Test
    fun `toJoml converts matrices with scale properly`() {
        val aiMatrix = AIMatrix4x4.create().apply {
            a1(2f); b1(0f); c1(0f); d1(0f)
            a2(0f); b2(3f); c2(0f); d2(0f)
            a3(0f); b3(0f); c3(4f); d3(0f)
            a4(0f); b4(0f); c4(0f); d4(1f)
        }

        val expected = Matrix4f().scale(2f, 3f, 4f)
        val result = aiMatrix.toJoml()

        assertEquals(expected, result)
        //assertMatrixAlmostEquals(expected, result)
    }

    @Test
    fun `toJoml converts matrices with rotation properly`() {
        val cos = 0f
        val sin = 1f

        val aiMatrix = AIMatrix4x4.create().apply {
            a1(cos); b1(0f); c1(-sin); d1(0f)
            a2(0f);  b2(1f); c2(0f);  d2(0f)
            a3(sin); b3(0f); c3(cos); d3(0f)
            a4(0f);  b4(0f); c4(0f);  d4(1f)
        }

        val expected = Matrix4f().rotateY(Math.toRadians(90.0).toFloat())
        val result = aiMatrix.toJoml()

        assert(expected.equals(result, EPSILON))
    }
}