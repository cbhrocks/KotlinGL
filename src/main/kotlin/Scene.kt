package org.kotlingl

import ShaderProgram
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lighting.Shader
import org.kotlingl.lights.Light
import org.kotlingl.shapes.Drawable
import org.kotlingl.shapes.GLResource
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

    fun initGL() {
        objects.mapNotNull { it as? GLResource }
            .forEach { it.initGL() }
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
): GLResource() {

    fun intersect(ray: Ray, context: RayTraceContext): Intersection? {
        require(layers.keys.containsAll(context.layersToCheck)) {
            "Layer(s) not found for ray tracing: ${context.layersToCheck - layers.keys}"
        }

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
        //return ColorRGB(50, 50, 50)
        return ColorRGB(50, 50, 50)
    }

    fun update(timeDelta: Float) {
        layers.filter { it.value.shouldUpdate }.forEach { it.value.update(timeDelta) }
    }

    override fun initGL() {
        layers.forEach { it.value.initGL() }
        markInitialized()
    }

    fun draw(shader: ShaderProgram, layersToCheck: Set<String>) {
        layers.filterKeys {
            it in layersToCheck
        } .values.flatMap{
            it.objects
        }.mapNotNull {
            it as? Drawable
        } .forEach {
            it.draw(shader)
        }
    }
}