package org.kotlingl.renderer

import org.kotlingl.Camera
import org.kotlingl.RayTraceContext
import org.kotlingl.Scene
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import java.nio.ByteBuffer

class ModelRenderer(var width: Int, var height: Int): Renderer {

    override fun render(
        scene: Scene,
        camera: Camera,
        target: Framebuffer
    ) {
    }
}

