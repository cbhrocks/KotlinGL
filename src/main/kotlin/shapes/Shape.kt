package org.kotlingl.shapes

interface Shape {

    val color: Vector3
    fun intersects(ray: Ray): Boolean

}