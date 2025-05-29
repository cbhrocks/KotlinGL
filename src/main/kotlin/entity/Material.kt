package org.kotlingl.entity


data class Material(
    var color: ColorRGB = ColorRGB(150, 150, 150),
    var reflect: Float = 0f,
    val shininess: Float = 1f,
) {
}