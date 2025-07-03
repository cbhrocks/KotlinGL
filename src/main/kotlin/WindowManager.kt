package org.kotlingl

import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.lang.AutoCloseable
import java.nio.IntBuffer
import kotlin.properties.Delegates

class WindowManager: AutoCloseable {
    var width = 1280
    var height = 720
    val resizeListeners = mutableListOf<(Int, Int) -> Unit>()
    var window by Delegates.notNull<Long>()

    fun initWindow() {
        // Configure GLFW
        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE) // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE) // the window will be resizable

        // Create the window
        window = GLFW.glfwCreateWindow(width, height, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL)
        GLFW.glfwSetWindowAspectRatio(window, width, height)
        if (window == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        GLFW.glfwSetKeyCallback(window, { window, key, scancode, action, mods ->
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) GLFW.glfwSetWindowShouldClose(
                window,
                true
            ) // We will detect this in the rendering loop
        })

        MemoryStack.stackPush().use { stack ->
            val pWidth: IntBuffer = stack.mallocInt(1) // int*
            val pHeight: IntBuffer = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            GLFW.glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidMode: GLFWVidMode? = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())

            if (vidMode == null) {
                throw RuntimeException("Failed to get video mode")
            }

            // Center the window
            GLFW.glfwSetWindowPos(
                window,
                (vidMode.width() - pWidth.get(0)) / 2,
                (vidMode.height() - pHeight.get(0)) / 2
            )
        }
        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window)

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        // Enable v-sync
        GLFW.glfwSwapInterval(1)

        // Make the window visible
        GLFW.glfwShowWindow(window)

        GLFW.glfwSetFramebufferSizeCallback(window) { _, w, h ->
            this.width = w
            this.height = h
            resizeListeners.forEach { it(w, h) }
        }
    }

    fun onResize(listener: (Int, Int) -> Unit) {
        resizeListeners += listener
    }

    fun swapBuffers() {
        GLFW.glfwSwapBuffers(window) // swap the color buffers
    }

    fun shouldClose(): Boolean {
        return GLFW.glfwWindowShouldClose(window)
    }

    fun pollEvents() {
        GLFW.glfwPollEvents()
    }

    override fun close() {
        Callbacks.glfwFreeCallbacks(window)
        GLFW.glfwDestroyWindow(window)
    }
}