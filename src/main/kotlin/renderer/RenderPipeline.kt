package org.kotlingl.renderer

import org.kotlingl.CameraManager
import org.kotlingl.Scene

class RenderPipeline(
    val background: BackgroundRenderer,
    val backgroundRayTrace: RayTraceRenderer,
    val world: WorldRenderer,
    val ui: UIRenderer,
    val compositor: Compositor,
    val debug: DebugRenderer? = null
) {
    val renderOrder = listOf("background", "world", "foreground", "debug", "ui")

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
            "debug" -> debug?.render(scene, cameras.getCamera("world"), compositor.getTarget("ui"))
        }
    }
}