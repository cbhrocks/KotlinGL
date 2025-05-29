package org.kotlingl.shapes

import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material

interface Shape {
    val material: Material
    fun intersects(ray: Ray): Intersection?

}