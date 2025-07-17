package org.kotlingl.renderer

import ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3f
import org.joml.Vector3fc
import org.kotlingl.Camera
import org.kotlingl.Collider.Capsule
import org.kotlingl.Collider.Circle
import org.kotlingl.Collider.Collider
import org.kotlingl.Collider.ColliderShape
import org.kotlingl.Collider.OBB2D
import org.kotlingl.Scene
import org.kotlingl.shapes.GLResource
import org.kotlingl.utils.checkGLError
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_LINES
import org.lwjgl.opengl.GL11.glDrawArrays
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glBufferSubData
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import kotlin.math.cos
import kotlin.math.sin

class DebugRenderer(
    private val shader: ShaderProgram
): Renderer, GLResource() {
    private val vertices = mutableListOf<Float>() // x, y, r, g, b
    private var vao = 0
    private var vbo = 0

    override fun initGL() {
        vao = glGenVertexArrays()
        vbo = glGenBuffers()

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, 10000 * 5 * 4L, GL_DYNAMIC_DRAW) // large enough

        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 5 * 4, 0L)
        glEnableVertexAttribArray(1)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 5 * 4, 2 * 4L)

        glBindVertexArray(0)
    }

    fun addLine(start: Vector2fc, end: Vector2fc, color: Vector3fc) {
        vertices.addAll(listOf(
            start.x(), start.y(), color.x(), color.y(), color.z(),
            end.x(), end.y(), color.x(), color.y(), color.z()
        ))
    }

    fun addCircle(center: Vector2fc, radius: Float, color: Vector3fc, segments: Int = 32) {
        val angleStep = (Math.PI * 2 / segments).toFloat()
        for (i in 0 until segments) {
            val theta1 = angleStep * i
            val theta2 = angleStep * (i + 1)

            val p1 = Vector2f(cos(theta1), sin(theta1)).mul(radius).add(center)
            val p2 = Vector2f(cos(theta2), sin(theta2)).mul(radius).add(center)
            addLine(p1, p2, color)
        }
    }

    fun drawCapsule(start: Vector2fc, end: Vector2fc, radius: Float, color: Vector3fc) {
        val dir = Vector2f(end).sub(start)
        val length = dir.length()
        if (length == 0f) {
            addCircle(start, radius, color)
            return
        }

        dir.normalize()
        val perpendicular = Vector2f(-dir.y, dir.x)

        val left1 = Vector2f(start).add(Vector2f(perpendicular).mul(radius))
        val right1 = Vector2f(start).sub(Vector2f(perpendicular).mul(radius))
        val left2 = Vector2f(end).add(Vector2f(perpendicular).mul(radius))
        val right2 = Vector2f(end).sub(Vector2f(perpendicular).mul(radius))

        addLine(left1, left2, color)
        addLine(right1, right2, color)
        addCircle(start, radius, color)
        addCircle(end, radius, color)
    }

    fun drawOBB(obb: OBB2D, color: Vector3fc) {
        val (center, halfExtents, axes) = obb

        val corners = listOf(
            Vector2f(-1f, -1f),
            Vector2f( 1f, -1f),
            Vector2f( 1f,  1f),
            Vector2f(-1f,  1f)
        ).map { corner ->
            Vector2f(center).add(
                Vector2f(axes[0]).mul(corner.x * halfExtents.x)
            ).add(
                Vector2f(axes[1]).mul(corner.y * halfExtents.y)
            )
        }

        for (i in 0 until 4) {
            addLine(corners[i], corners[(i + 1) % 4], color)
        }
    }

    fun drawColliderShape(shape: ColliderShape, color: Vector3fc) {
        when (shape) {
            is OBB2D -> drawOBB(shape, color)
            is Circle -> addCircle(shape.center, shape.radius, color)
            is Capsule -> drawCapsule(shape.start, shape.end, shape.radius, color)
        }
    }

    fun drawCollider(collider: Collider) {
        drawColliderShape(collider.shape, Vector3f(1.0f, 0f, 0f))
    }

    override fun render(scene: Scene, camera: Camera, target: Framebuffer) {
        scene.colliders.forEach { drawCollider(it) }

        target.withBind {
            glBindVertexArray(vao)
            glBindBuffer(GL_ARRAY_BUFFER, vbo)

            val vertexArray = vertices.toFloatArray()
            val buffer = BufferUtils.createFloatBuffer(vertexArray.size).put(vertexArray).flip()
            glBufferSubData(GL_ARRAY_BUFFER, 0L, buffer)

            shader.use()
            camera.bind(shader, target.width.toFloat()/target.height.toFloat())

            glDrawArrays(GL_LINES, 0, vertexArray.size / 5)

            glBindVertexArray(0)
        }
        checkGLError("Debug render")

        vertices.clear()
    }
}