package org.kotlingl.renderer

import ShaderProgram
import org.kotlingl.Camera
import org.kotlingl.Scene

class BackgroundRenderer(
    private val shader: ShaderProgram
): Renderer {

    override fun render(scene: Scene, camera: Camera, target: Framebuffer) {
        shader.use()
        camera.bind(shader, target.width.toFloat()/target.height.toFloat())

        scene.draw(shader, setOf("background"))
    }
}