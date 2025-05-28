package org.kotlingl.shapes

import org.kotlingl.entity.ColorRGB
import org.kotlingl.math.Vector3
import org.kotlingl.math.dot
import org.kotlingl.math.minus

class Circle(
    val center: Vector3 = Vector3(0f,0f,0f),
    val radius: Float = 1f,
    override val color: ColorRGB
): Shape {

    override fun intersects(ray: Ray): Boolean {
        val oc = ray.origin - this.center
        val a = ray.direction dot ray.direction
        val b = 2.0 * (oc dot ray.direction)
        val c = (oc dot oc) - this.radius * this.radius

        val discriminant = b * b - 4 * a * c
        return discriminant >= 0
    }
}