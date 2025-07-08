package org.kotlingl.renderer

import org.kotlingl.CameraManager
import org.kotlingl.Scene

class RenderPipeline(
    val background: BackgroundRenderer,
    val backgroundRayTrace: RayTraceRenderer,
    val world: WorldRenderer,
    val ui: UIRenderer,
    val compositor: Compositor
) {
    val renderOrder = listOf("background_far", "background_near", "world", "foreground", "ui")

    fun render(scene: Scene, cameras: CameraManager) {
        compositor.clearBuffers()

        background.render(scene, cameras.getCamera("background"), compositor.getTarget("background"))
        // backgroundRayTrace.render(scene, cameras.getCamera("background"), compositor.getTarget("background"))
        world.render(scene, cameras.getCamera("world"), compositor.getTarget("world"))
        ui.render(scene, cameras.getCamera("ui"), compositor.getTarget("ui"))

        compositor.composeToScreen()
    }
}