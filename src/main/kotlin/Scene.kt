package org.kotlingl

import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lighting.Shader
import org.kotlingl.lights.Light
import org.kotlingl.shapes.Intersectable
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Updatable

class Layer(
    val name: String,
    val objects: MutableList<Any>,
    val camera: Camera? = null,
    val visible: Boolean = true,
    val shouldUpdate: Boolean = true,
) {
    fun update(timeDelta: Float) {
        objects
            .mapNotNull { it as? Updatable }
            .forEach { it.update(timeDelta) }
    }
}

data class RayTraceContext(
    val scene: Scene,
    val camera: Camera,
    val layersToCheck: Set<String>,
    val recursionDepth: Int = 0
)

data class Scene(
    var shader: Shader,
    var cameraManager: CameraManager,
    var lights: MutableList<Light> = mutableListOf<Light>(),
    var layers: MutableMap<String, Layer> = mutableMapOf(),
    var activeCameraIndex: Int = 0
) {
    fun intersect(ray: Ray, context: RayTraceContext): Intersection? {
        return layers.filterKeys{ it in context.layersToCheck }.values
            .flatMap{it.objects}
            .mapNotNull { (it as? Intersectable)?.intersects(ray) }
            .minByOrNull { it.t }
    }

    fun traceRay(ray: Ray, context: RayTraceContext): ColorRGB {
        val intersection = this.intersect(ray, context)
        if (intersection != null){
            //return intersection.material.color
            return shader.shade(intersection, context)
        }
        return ColorRGB(50, 50, 50)
    }

    fun update(timeDelta: Float) {
        layers.filter { it.value.shouldUpdate }.forEach { it.value.update(timeDelta) }
    }
}