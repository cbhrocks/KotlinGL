package org.kotlingl.renderer

import org.kotlingl.Camera
import org.kotlingl.DevTools
import org.kotlingl.Scene

class UIRenderer(var devTools: DevTools? = null): Renderer {

    override fun render(scene: Scene, camera: Camera, target: Framebuffer) {
        target.withBind {
            devTools?.render()
        }
    }
}