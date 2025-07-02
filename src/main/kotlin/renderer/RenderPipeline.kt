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
        background.render(scene, cameras.getCamera("background"), compositor.backgroundFB)
        backgroundRayTrace.render(scene, cameras.getCamera("background"), compositor.backgroundFB)
        world.render(scene, cameras.getCamera("world"), compositor.worldFB)
        ui.render(scene, cameras.getCamera("ui"), compositor.uiFB)

        compositor.composeToScreen()
    }
}