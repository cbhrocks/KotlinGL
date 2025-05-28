package org.kotlingl.Entity

import org.kotlingl.entity.ColorRGB


data class Material(
    var color: ColorRGB = ColorRGB(150, 150, 150),
    var reflect: Float = 0f,
) {
}