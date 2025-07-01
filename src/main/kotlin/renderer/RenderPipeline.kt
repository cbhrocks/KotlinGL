package org.kotlingl.renderer

import org.kotlingl.CameraManager
import org.kotlingl.Scene

class RenderPipeline(
    val background: BackgroundRenderer,
    val world: WorldRenderer,
    val model: ModelRenderer,
    val ui: UIRenderer,
    val compositor: Compositor
) {
    fun render(scene: Scene, cameras: CameraManager) {
        background.render(scene, cameras.background, compositor.backgroundFB)
        world.render(scene, cameras.world, compositor.worldFB)
        model.render(scene, cameras.world, compositor.worldFB)
        ui.render(scene, cameras.ui, compositor.uiFB)

        compositor.composeToScreen()
    }
}