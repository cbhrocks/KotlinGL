package org.kotlingl.entity

import org.kotlingl.math.Vector3

data class ColorRGB(
    var red: Int,
    var green: Int,
    var blue: Int,
) {
    var r: Int get() = red; set(value) { red = value }
    var g: Int get() = green; set(value) { green = value }
    var b: Int get() = blue; set(value) { blue = value }

    init {
        require(this.red in 0..255) { "Red value ($red) must be between 0 and 255." }
        require(this.green in 0..255) { "Green value ($green) must be between 0 and 255." }
        require(this.blue in 0..255) { "Blue value ($blue) must be between 0 and 255." }
    }

    companion object {
        val BLACK = ColorRGB(0, 0, 0)
        val WHITE = ColorRGB(255, 255, 255)
        val RED = ColorRGB(255, 0, 0)
        val GREEN = ColorRGB(0, 255, 0)
        val BLUE = ColorRGB(0, 0, 255)
    }

    override fun toString(): String {
        return "R: $r, G: $g B: $b"
    }
}

fun Vector3.toColor(): ColorRGB =
    ColorRGB(
        (x * 255f).toInt().coerceIn(0, 255),
        (y * 255f).toInt().coerceIn(0, 255),
        (z * 255f).toInt().coerceIn(0, 255)
    )