import org.lwjgl.opengl.GL20.*
import org.joml.Matrix4f
import org.joml.Vector2fc
import org.joml.Vector3f
import java.nio.FloatBuffer
import org.lwjgl.system.MemoryStack

class ShaderProgram(
    vertexSource: String,
    fragmentSource: String
) {
    val programId: Int

    init {
        val vertexShaderId = compileShader(vertexSource, GL_VERTEX_SHADER)
        val fragmentShaderId = compileShader(fragmentSource, GL_FRAGMENT_SHADER)

        programId = glCreateProgram()
        glAttachShader(programId, vertexShaderId)
        glAttachShader(programId, fragmentShaderId)
        glLinkProgram(programId)

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw RuntimeException("Shader program linking failed: ${glGetProgramInfoLog(programId)}")
        }

        glDeleteShader(vertexShaderId)
        glDeleteShader(fragmentShaderId)
    }

    fun use() {
        glUseProgram(programId)
    }

    fun setUniform(name: String, value: Int) {
        glUniform1i(getUniformLocation(name), value)
    }

    fun setUniform(name: String, value: Float) {
        glUniform1f(getUniformLocation(name), value)
    }

    fun setUniform(name: String, vector: Vector2fc) {
        glUniform2f(getUniformLocation(name), vector.x(), vector.y())
    }

    fun setUniform(name: String, vector: Vector3f) {
        glUniform3f(getUniformLocation(name), vector.x, vector.y, vector.z)
    }

    fun setUniform(name: String, matrix: Matrix4f) {
        MemoryStack.stackPush().use { stack ->
            val buffer: FloatBuffer = stack.mallocFloat(16)
            matrix.get(buffer)
            glUniformMatrix4fv(getUniformLocation(name), false, buffer)
        }
    }

    private fun getUniformLocation(name: String): Int {
        val location = glGetUniformLocation(programId, name)
        if (location == -1) {
            System.err.println("Warning: Uniform '$name' not found in shader.")
        }
        return location
    }

    fun destroy() {
        glDeleteProgram(programId)
    }

    private fun compileShader(source: String, type: Int): Int {
        val shaderId = glCreateShader(type)
        glShaderSource(shaderId, source)
        glCompileShader(shaderId)

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw RuntimeException("Shader compilation failed: ${glGetShaderInfoLog(shaderId)}")
        }

        return shaderId
    }

    companion object {
        fun loadShaderSource(path: String): String {
            val resource = ShaderProgram::class.java.getResourceAsStream(path)
                ?: throw IllegalArgumentException("Shader file not found: $path")
            return resource.bufferedReader().use { it.readText() }
        }
    }
}