package org.kotlingl

import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.lighting.Shader
import org.kotlingl.lights.Light
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Shape

data class Scene(
    var shader: Shader,
    var cameras: MutableList<Camera> = mutableListOf<Camera>(Camera()),
    var lights: MutableList<Light> = mutableListOf<Light>(),
    var shapes: MutableList<Shape> = mutableListOf<Shape>(),
    var activeCameraIndex: Int = 0
) {
    val activeCamera
        get() = this.cameras[this.activeCameraIndex]

    fun initGL() {
        for (camera in cameras) {
            camera.initGL()
        }
    }

    fun intersect(ray: Ray): Intersection? {
        var closestHit: Intersection? = null
        for (shape in shapes) {
            val hit = shape.intersects(ray)
            if (hit != null) {
                if (closestHit == null || hit.t < closestHit.t) {
                    closestHit = hit
                }
            }
        }
        return closestHit
    }

    fun traceRay(ray: Ray, depth: Int = 0): ColorRGB {
        val intersection = this.intersect(ray)
        if (intersection != null){
            //return intersection.material.color
            return shader.shade(intersection, this, depth)
        }
        return ColorRGB.BLACK
    }
}