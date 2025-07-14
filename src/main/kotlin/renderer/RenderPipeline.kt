package org.kotlingl.renderer

import org.kotlingl.CameraManager
import org.kotlingl.DevTools
import org.kotlingl.Scene

class RenderPipeline(
    val background: BackgroundRenderer,
    val backgroundRayTrace: RayTraceRenderer,
    val world: WorldRenderer,
    val ui: UIRenderer,
    val compositor: Compositor,
) {
    val renderOrder = listOf("background", "world", "foreground", "ui", "dev")

    fun render(scene: Scene, cameras: CameraManager) {
        compositor.clearBuffers()
        renderOrder.forEach {
            renderByName(it, scene, cameras)
        }
        compositor.composeToScreen()
    }

    fun renderByName(name: String, scene: Scene, cameras: CameraManager) {
        when (name) {
            "background" -> background.render(scene, cameras.getCamera("background"), compositor.getTarget("background"))
            "world" -> world.render(scene, cameras.getCamera("world"), compositor.getTarget("world"))
            "ui" -> ui.render(scene, cameras.getCamera("ui"), compositor.getTarget("ui"))
        }
    }
}