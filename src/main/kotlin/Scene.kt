package org.kotlingl

import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lighting.Shader
import org.kotlingl.lights.Light
import org.kotlingl.shapes.Intersectable
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Updatable

data class Scene(
    var shader: Shader,
    var cameraManager: CameraManager,
    var lights: MutableList<Light> = mutableListOf<Light>(),
    var sceneObjects: MutableList<Any> = mutableListOf(),
    var activeCameraIndex: Int = 0
) {
    fun initGL() {
        cameraManager.initCameras()
    }

    fun intersect(ray: Ray): Intersection? {
        return sceneObjects.mapNotNull { (it as? Intersectable)?.intersects(ray) }
            .minByOrNull { it.t }
    }

    fun traceRay(ray: Ray, depth: Int = 0): ColorRGB {
        val intersection = this.intersect(ray)
        if (intersection != null){
            //return intersection.material.color
            return shader.shade(intersection, this, depth)
        }
        return ColorRGB(50, 50, 50)
    }

    fun update(timeDelta: Float) {
        return sceneObjects.mapNotNull { it as? Updatable }
            .forEach { it.update(timeDelta) }
    }
}