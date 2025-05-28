package org.kraytracer

import org.kraytracer.shapes.Ray
import org.kraytracer.shapes.Shape
import org.kraytracer.shapes.Vector3

data class Scene(
    var cameras: MutableList<Camera> = mutableListOf<Camera>(Camera()),
    var shapes: MutableList<Shape> = mutableListOf<Shape>(),
    var activeCamera: Int = 0
) {
    fun initGL() {
        for (camera in cameras) {
            camera.initGL()
        }
    }

    fun traceRay(ray: Ray): Vector3 {
        for (shape in this.shapes) {
            if (shape.intersects(ray)) {
                return shape.color
            }
        }
        return Vector3(0f, 0f, 0f)
    }

    fun getActiveCamera(): Camera {
        return this.cameras[this.activeCamera]
    }
}