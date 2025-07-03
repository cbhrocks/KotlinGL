package org.kotlingl.shapes

import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil.memAllocFloat
import org.lwjgl.system.MemoryUtil.memFree
import kotlin.properties.Delegates

class ScreenQuad {
    var vaoId by Delegates.notNull<Int>()
    var vboId by Delegates.notNull<Int>()

    fun initGL() {
        vaoId = glGenVertexArrays()
        vboId = glGenBuffers()

        glBindVertexArray(vaoId)

        val vertices = floatArrayOf(
            // triangle 1
            -1f,  1f, 0f, 1f,  // top-left
            -1f, -1f, 0f, 0f,  // bottom-left
            1f, -1f, 1f, 0f,  // bottom-right
            // triangle 2
            -1f,  1f, 0f, 1f,  // top-left
            1f, -1f, 1f, 0f,  // bottom-right
            1f,  1f, 1f, 1f   // top-right
        )

        val buffer = memAllocFloat(vertices.size).put(vertices).flip()

        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)

        val stride = 4 * java.lang.Float.BYTES
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L)
        glEnableVertexAttribArray(0)

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, (2 * java.lang.Float.BYTES).toLong())
        glEnableVertexAttribArray(1)

        glBindVertexArray(0)
        memFree(buffer)
    }

    fun render() {
        glBindVertexArray(vaoId)
        glDrawArrays(GL_TRIANGLES, 0, 6)
        glBindVertexArray(0)
    }

    fun destroy() {
        glDeleteBuffers(vboId)
        glDeleteVertexArrays(vaoId)
    }
}