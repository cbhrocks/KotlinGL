package org.kotlingl.model

import ShaderProgram
import org.joml.Vector2fc
import org.joml.Vector3f
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.shapes.AABB
import org.kotlingl.shapes.Bounded
import org.kotlingl.shapes.Drawable
import org.kotlingl.shapes.GLResource
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Triangle
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL11.glDrawElements
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_STATIC_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glDeleteVertexArrays
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.system.MemoryUtil

data class Vertex (
    val position: Vector3f,
    val normal: Vector3f,
    val uv: Vector2fc,
    // Max 4 bones per vertex
    val boneIndices: List<Int> = listOf(),
    val boneWeights: List<Float> = listOf()
)

class Mesh(
    val vertices: List<Vertex>,
    val indices: List<Int>,
    val material: Material,
    val bones: List<Bone> = listOf()
): GLResource(), Bounded, Drawable {
    var vaoId: Int = 0
    var vboId: Int = 0
    var eboId: Int = 0

    val triangles: List<Triangle> by lazy {
        indices.chunked(3).map { (i0, i1, i2) ->
            Triangle(
                vertices[i0],
                vertices[i1],
                vertices[i2],
                material
            )
        }
    }

    init {
        require(indices.size >= 3) { "A mesh must have vertices associated with it." }
        for (i in indices) {
            require(i in 0..vertices.size)
        }
    }

    override fun initGL() {
        material.initGL()

        vaoId = glGenVertexArrays()
        vboId = glGenBuffers()
        eboId = glGenBuffers()

        glBindVertexArray(vaoId)

        // Flatten the vertex data
        val vertexData = MemoryUtil.memAllocFloat(vertices.size * STRIDE)
        for (v in vertices) {
            vertexData.put(v.position.x).put(v.position.y).put(v.position.z)
            vertexData.put(v.normal.x).put(v.normal.y).put(v.normal.z)
            vertexData.put(v.uv.x()).put(v.uv.y())

            // Pad bone indices and weights to 4
            val paddedIndices = v.boneIndices + List(4 - v.boneIndices.size) { 0 }
            val paddedWeights = v.boneWeights + List(4 - v.boneWeights.size) { 0f }

            paddedIndices.forEach { vertexData.put(it.toFloat()) }
            paddedWeights.forEach { vertexData.put(it) }
        }
        vertexData.flip()

        // Upload vertex data
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)
        MemoryUtil.memFree(vertexData)

        // Upload index data
        val indexBuffer = MemoryUtil.memAllocInt(indices.size)
        indexBuffer.put(indices.toIntArray()).flip()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW)
        MemoryUtil.memFree(indexBuffer)

        val strideBytes = STRIDE * java.lang.Float.BYTES
        var offset = 0L

        // position
        glVertexAttribPointer(0, 3, GL_FLOAT, false, strideBytes, offset)
        glEnableVertexAttribArray(0)
        offset += 3 * 4

        // normal
        glVertexAttribPointer(1, 3, GL_FLOAT, false, strideBytes, offset)
        glEnableVertexAttribArray(1)
        offset += 3 * 4

        // uv
        glVertexAttribPointer(2, 2, GL_FLOAT, false, strideBytes, offset)
        glEnableVertexAttribArray(2)
        offset += 2 * 4

        // bone indices
        glVertexAttribPointer(3, 4, GL_FLOAT, false, strideBytes, offset)
        glEnableVertexAttribArray(3)
        offset += 4 * 4

        // bone weights
        glVertexAttribPointer(4, 4, GL_FLOAT, false, strideBytes, offset)
        glEnableVertexAttribArray(4)

        glBindVertexArray(0)

        markInitialized()
    }

    override fun cleanupGL() {
        glDeleteVertexArrays(vaoId)
        glDeleteBuffers(vboId)
        glDeleteBuffers(eboId)
    }

    override fun draw(shader: ShaderProgram) {
        material.bind(shader)
        glBindVertexArray(vaoId)
        glDrawElements(GL_TRIANGLES, indices.size, GL_UNSIGNED_INT, 0)
        glBindVertexArray(0)
    }

    override fun intersects(ray: Ray): Intersection? {
        //return this.bvhNode.intersects(ray)
        return this.triangles.mapNotNull { it.intersects(ray) }.minByOrNull { it.t }
    }

    override fun computeAABB(): AABB {
        //return this.bvhNode.localAABB

        val min = Vector3f(Float.POSITIVE_INFINITY)
        val max = Vector3f(Float.NEGATIVE_INFINITY)

        for (v in vertices) {
            min.min(v.position)
            max.max(v.position)
        }

        return AABB(min, max)
    }

    //override fun getBVHNode(): BVHNode {
    //    return this.bvhNode
    //}

    override fun centroid(): Vector3f {
        val center = Vector3f()
        for (tri in triangles) {
            center.add(tri.centroid())
        }
        return center.div(triangles.size.toFloat())
    }

    companion object {
        const val STRIDE = 3 + 3 + 2 + 4 + 4  // pos, normal, uv, boneIdx, boneWeight
    }
}