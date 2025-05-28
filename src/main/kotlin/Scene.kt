package org.kotlingl

import org.kotlingl.entity.ColorRGB
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Shape
import org.kotlingl.math.Vector3

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

    fun traceRay(ray: Ray): ColorRGB {
        for (shape in this.shapes) {
            if (shape.intersects(ray)) {
                return shape.color
            }
        }
        return ColorRGB.BLACK
    }

    fun getActiveCamera(): Camera {
        return this.cameras[this.activeCamera]
    }
}