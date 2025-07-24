package org.kotlingl.renderer

import org.kotlingl.Camera
import org.kotlingl.Scene
import org.kotlingl.utils.checkGLError

class UIRenderer(): Renderer {

    override fun render(scene: Scene, camera: Camera, target: Framebuffer) {
        target.withBind {
        }
        checkGLError("UIRenderer render")
    }
}