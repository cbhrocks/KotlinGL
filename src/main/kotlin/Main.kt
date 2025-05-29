package org.kotlingl

import org.kotlingl.entity.ColorRGB
import org.kotlingl.math.Vector3
import org.kotlingl.shapes.Sphere
import org.kotlingl.entity.Material
import org.kotlingl.lighting.*
import org.kotlingl.lights.DirectionalLight
import org.kotlingl.shapes.Plane

fun main() {
    val width = 480
    val height = 240

    val scene = Scene(
        shader = Shader.Builder()
            .addStage(DiffuseStage())
            .addStage(SpecularStage())
            .build(),
        cameras = mutableListOf(
            Camera(
                Vector3.UNIT_Y,
                lookAt = Vector3(0f, 1f, -1f),
                resX = width,
                resY = height,
            )
        ),
        lights = mutableListOf(
            AmbientLight(
                ColorRGB.WHITE,
                0.1f,
            ),
            PointLight(
                ColorRGB.WHITE,
                Vector3(-2f, 3f, -2f),
                0.5f
            ),
            DirectionalLight(
                ColorRGB.WHITE,
                Vector3(2f, -2f, -2f),
                0.2f
            )
        ),
        shapes = mutableListOf(
            Sphere(
                Vector3(-3f, 1f, -3f),
                1f,
                Material(
                    ColorRGB.RED
                )
            ),
            Sphere(
                Vector3(0f, 1f, -3f),
                1f,
                Material(
                    ColorRGB.GREEN
                )
            ),
            Sphere(
                Vector3(3f, 1f, -3f),
                1f,
                Material(
                    ColorRGB.BLUE
                )
            ),
            Plane(
                Vector3.ZERO,
                Vector3.UNIT_Y,
                Material(
                    ColorRGB.GREY
                )
            )
        ),
    )

    Renderer(scene, width, height).run()
}