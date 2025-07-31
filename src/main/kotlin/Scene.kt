package org.kotlingl

import ShaderProgram
import org.kotlingl.Collider.Collider
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lighting.Shader
import org.kotlingl.lights.Light
import org.kotlingl.model.Model
import org.kotlingl.shapes.Drawable
import org.kotlingl.shapes.GLResource
import org.kotlingl.shapes.Intersectable
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Updatable
import java.util.SortedMap

class LayerObject(val obj: Any) {
    val updatable: Updatable? = obj as? Updatable
    val drawable: Drawable? = obj as? Drawable
    val glResource: GLResource? = obj as? GLResource
    val model: Model? = obj as? Model
}

class Layer(
    val name: String,
    val objects: MutableList<LayerObject>,
    val camera: Camera? = null,
    val visible: Boolean = true,
    val shouldUpdate: Boolean = true,
) {
    fun update(timeDelta: Float) {
        objects.forEach { it.updatable?.update(timeDelta) }
    }

    fun initGL() {
        objects.forEach { it.glResource?.initGL() }
    }

    fun addObject(obj: LayerObject) {
        this.objects.addLast(obj)
    }
}

data class RayTraceContext(
    val scene: Scene,
    val camera: Camera,
    val layersToCheck: Set<String>,
    val recursionDepth: Int = 0
)

object CollisionLayers {
    const val PLAYER = 1 shl 0
    const val ENEMY = 1 shl 1
    const val ENVIRONMENT = 1 shl 2
    const val PROJECTILE = 1 shl 3
}

data class Scene(
    var shader: Shader,
    var cameraManager: CameraManager,
    var lights: MutableList<Light> = mutableListOf(),
    var layers: SortedMap<String, Layer> = sortedMapOf(),
    var colliders: MutableList<Collider> = mutableListOf()
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
        }.values.flatMap{
            it.objects
        }.forEach {
            it.drawable?.draw(shader)
        }
    }

    fun getLayerNames() = layers.keys.toList()

    fun getAllObjects() = layers.values.flatMap { it.objects }

    fun addObject(layerName: String, obj: Any) {
        layers.getValue(layerName).addObject(LayerObject(obj))
    }
}