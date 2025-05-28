package org.kotlingl

import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Shape
import org.kotlingl.shapes.Vector3
import org.kraytracer.Scene
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.IntBuffer

class Renderer(val scene: Scene) {
    // The window handle
    private var window: Long = 0

    init {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE) // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE) // the window will be resizable

        // Create the window
        window = GLFW.glfwCreateWindow(480, 240, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        GLFW.glfwSetKeyCallback(window, { window, key, scancode, action, mods ->
            if (key === GLFW.GLFW_KEY_ESCAPE && action === GLFW.GLFW_RELEASE) GLFW.glfwSetWindowShouldClose(
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
        // Enable v-sync
        GLFW.glfwSwapInterval(1)

        // Make the window visible
        GLFW.glfwShowWindow(window)

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()
        this.scene.initGL()
    }

    fun run() {
        println("Hello LWJGL " + Version.getVersion() + "!")

        loop()

        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(window)
        GLFW.glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)?.free()
    }

    private fun traceRays(camera: Camera, width: Int, height: Int) {
        val rays = camera.generateRays()

        // create pixel buffer
        val pixels = ByteBuffer.allocateDirect(width * height * 3)
        // put colors for each ray in pixel buffer
        rays.forEachIndexed { index, ray ->
            val color = this.scene.traceRay(ray)
            pixels.put(color.r.toByte())
            pixels.put(color.g.toByte())
            pixels.put(color.b.toByte())
            //pixels.put((color.x * 255f).toInt().coerceIn(0, 255).toByte())
            //pixels.put((color.y * 255f).toInt().coerceIn(0, 255).toByte())
            //pixels.put((color.z * 255f).toInt().coerceIn(0, 255).toByte())
        }
        // upload pixel buffer to texture
        pixels.rewind()

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, camera.textureId!!)
        GL11.glTexSubImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            0,
            0,
            width,
            height,
            GL11.GL_RGB,
            GL11.GL_UNSIGNED_BYTE,
            pixels
        )

        // Draw texture on full-screen quad
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(-1f, -1f)
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f( 1f, -1f)
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f( 1f,  1f)
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(-1f,  1f)
        GL11.glEnd()
    }

    private fun loop() {
        // Set the clear color
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!GLFW.glfwWindowShouldClose(window)) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT) // clear the framebuffer

            traceRays(this.scene.getActiveCamera(), 480, 240)

            GLFW.glfwSwapBuffers(window) // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            GLFW.glfwPollEvents()
        }
    }
}