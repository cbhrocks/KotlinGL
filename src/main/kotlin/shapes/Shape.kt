package org.kotlingl.shapes

import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material

interface Shape {
    fun intersects(ray: Ray): Intersection?
}