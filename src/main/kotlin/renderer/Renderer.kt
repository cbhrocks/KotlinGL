package org.kotlingl.renderer

import org.kotlingl.Camera
import org.kotlingl.Scene

interface Renderer {
    fun render(scene: Scene, camera: Camera, target: Framebuffer)
}