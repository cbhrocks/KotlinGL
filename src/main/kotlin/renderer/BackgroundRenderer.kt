package org.kotlingl.renderer

import ShaderProgram
import org.kotlingl.Camera
import org.kotlingl.Scene
import org.kotlingl.utils.checkGLError
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glEnable

class BackgroundRenderer(
    private val shader: ShaderProgram
): Renderer {

    override fun render(scene: Scene, camera: Camera, target: Framebuffer) {
        shader.use()
        target.withBind {
            camera.bind(shader, target.width.toFloat()/target.height.toFloat())
            glEnable(GL_DEPTH_TEST)
            scene.draw(shader, setOf("background"))
            glDisable(GL_DEPTH_TEST)
        }
        checkGLError("BackgroundRendered render")
    }
}