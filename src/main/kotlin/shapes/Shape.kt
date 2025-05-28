package org.kotlingl.shapes

import org.kotlingl.entity.ColorRGB

interface Shape {

    val color: ColorRGB
    fun intersects(ray: Ray): Boolean

}