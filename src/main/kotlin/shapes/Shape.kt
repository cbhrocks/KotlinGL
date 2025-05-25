package org.kraytracer.shapes

interface Shape {

    val color: Vector3
    fun intersects(ray: Ray): Boolean

}