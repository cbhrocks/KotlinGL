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

data class Scene(
    var shader: Shader,
    var cameraManager: CameraManager,
    var lights: MutableList<Light> = mutableListOf<Light>(),
    var layers: MutableMap<String, Layer> = mutableMapOf(),
    var activeCameraIndex: Int = 0
) {
    fun intersect(ray: Ray, layersToCheck: Set<String>): Intersection? {
        return layers.filterKeys{ it in layersToCheck }.values
            .flatMap{it.objects}
            .mapNotNull { (it as? Intersectable)?.intersects(ray) }
            .minByOrNull { it.t }
    }

    fun traceRay(ray: Ray, layersToCheck: Set<String>, depth: Int = 0): ColorRGB {
        val intersection = this.intersect(ray, layersToCheck)
        if (intersection != null){
            //return intersection.material.color
            return shader.shade(intersection, this, depth, camera)
        }
        return ColorRGB(50, 50, 50)
    }

    fun update(timeDelta: Float) {
        layers.filter { it.value.shouldUpdate }.forEach { it.value.update(timeDelta) }
    }
}