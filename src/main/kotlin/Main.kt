package org.kotlingl

import org.kotlingl.entity.ColorRGB
import org.kotlingl.math.Vector3
import org.kotlingl.shapes.Sphere
import org.kotlingl.entity.Material
import org.kotlingl.entity.Texture
import org.kotlingl.entity.WrapMode
import org.kotlingl.lighting.*
import org.kotlingl.lights.DirectionalLight
import org.kotlingl.math.Vector2
import org.kotlingl.math.unaryMinus
import org.kotlingl.model.Vertex
import org.kotlingl.shapes.Plane
import org.kotlingl.shapes.Triangle
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    //val width = 480
    val width = 1920
    //val height = 240
    val height = 1080

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
                0.7f
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
                    ColorRGB.GREEN,
                    texture=Texture.fromImageFile("/textures/numbered-checker.png"),
                    //shininess=0f
                ),
                right = -Vector3.UNIT_X
            ),
            Sphere(
                Vector3(3f, 1f, -3f),
                1f,
                Material(
                    ColorRGB.BLUE
                )
            ),
            Triangle(
                Vertex(
                    Vector3(-3f, 3f, -3f),
                    Vector3.UNIT_Z,
                    Vector2(0f, 0f),
                ),
                Vertex(
                    Vector3(3f, 3f, -3f),
                    Vector3.UNIT_Z,
                    Vector2(1f, 0f),
                ),
                Vertex(
                    Vector3(0f, 4f, -3f),
                    Vector3.UNIT_Z,
                    Vector2(.5f, 1f),
                ),
                Material(
                    texture=Texture.fromImageFile("/textures/numbered-checker.png")
                ),
            ),
            Plane(
                Vector3(-.5f, 0f, -3f),
                Vector3.UNIT_Y,
                Material(
                    ColorRGB.GREY,
                    texture= Texture.fromImageFile("/textures/cement-1.png"),
                    uvScale = Vector2(5f, 5f),
                    wrapMode = WrapMode.MIRROR,
                ),
                Vector3.UNIT_X
            )
        ),
    )

    Renderer(scene, width, height).run()
}