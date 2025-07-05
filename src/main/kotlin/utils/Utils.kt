package org.kotlingl.utils

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION
import java.nio.file.Path


fun Path.toUnixString(): String {
    return "/" + this.joinToString { "/" }
}

fun isGLReady(): Boolean {
    return GLFW.glfwGetCurrentContext() != 0L && GL.getCapabilities() != null
}

fun checkGLError(context: String = "") {
    var error: Int
    while (glGetError().also { error = it } != GL_NO_ERROR) {
        val errorMessage = when (error) {
            GL_INVALID_ENUM -> "GL_INVALID_ENUM"
            GL_INVALID_VALUE -> "GL_INVALID_VALUE"
            GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
            GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW"
            GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW"
            GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
            GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
            else -> "Unknown OpenGL error: 0x${Integer.toHexString(error)}"
        }
        System.err.println("OpenGL Error [$context]: $errorMessage")
    }
}