package org.kotlingl

import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.times
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Material
import org.kotlingl.entity.Texture
import org.kotlingl.entity.WrapMode
import org.kotlingl.lighting.*
import org.kotlingl.lights.DirectionalLight
import org.kotlingl.model.Model
import org.kotlingl.model.ModelLoader
import org.kotlingl.model.Vertex
import org.kotlingl.shapes.Plane
import org.kotlingl.shapes.Sphere
import org.kotlingl.shapes.Triangle
import kotlin.math.PI

fun main() {
    val width = 480
    //val width = 1920
    val height = 240
    //val height = 1080
    val ml = ModelLoader()
    ml.loadModel("/models/spider/spider.obj", "spider")
    ml.loadModel("/models/box/box.obj", "box")
    ml.loadModel("/models/blocky-characters/FBX format/character-a.fbx", "blocky-character-a")
    //ml.loadModel("/models/blocky-characters/OBJ format/character-a.obj", "blocky-character-a")
    val spider = ml.createModel("spider")
    spider.transform(
        rotation = Quaternionf().rotateY(-PI.toFloat()/2f),
        scale = Vector3f(.02f, .02f, .02f)
    )
    val box = ml.createModel("box")
    val blockyChar = ml.createModel("blocky-character-a")
    blockyChar.transform(
        //vector3f()
        scale = Vector3f(.5f, .5f, .5f),
        rotation = Quaternionf().rotateY(PI.toFloat())//.rotateX(PI.toFloat()/2f),
    )

    val scene = Scene(
        shader = Shader.Builder()
            .addStage(AmbientStage(1f))
            .addStage(DiffuseStage())
            .addStage(SpecularStage())
            .build(),
        cameras = mutableListOf(
            Camera(
                // Vector3f(-1f, 1f, 120f),
                // lookAt = Vector3f(0f, 0f, 0f),
                //Vector3f(-0f, 50f, 200f),
                Vector3f(0f, 1f, -2f),
                lookAt = Vector3f(0f, 0f, 0f),
                resX = width,
                resY = height,
            )
        ),
        lights = mutableListOf(
            //PointLight(
            //    ColorRGB.WHITE,
            //    Vector3f(-2f, 3f, -2f),
            //    0.5f
            //),
            //DirectionalLight(
            //    ColorRGB.WHITE,
            //    Vector3f(2f, -2f, -2f),
            //    0.7f
            //)
        ),
        shapes = mutableListOf(
            //spider,
            //box,
            blockyChar,
            //Model.fromAssimp("/models/box/box.obj").apply {
            //    transform(
            //        Vector3f(0f, 1f, -3f),
            //        //Quaternionf().rotateY(PI.toFloat()),
            //        //scale= Vector3f(.2f, .2f, .2f)
            //    )
            //},
            //Sphere(
            //    Vector3f(-3f, 1f, -3f),
            //    1f,
            //    Material(
            //        ColorRGB.RED
            //    )
            //),
            //Sphere(
            //    Vector3f(0f, 1f, -3f),
            //    1f,
            //    Material(
            //        ColorRGB.GREEN,
            //        texture= Texture.fromImageFile("/textures/numbered-checker.png"),
            //        //shininess=0f
            //    ),
            //    up = Vector3f(0f, 1f, 0f),
            //    right = Vector3f(1f, 0f, 0f)
            //),
            //Sphere(
            //    Vector3f(3f, 1f, -3f),
            //    1f,
            //    Material(
            //        ColorRGB.BLUE
            //    )
            //),
            //Triangle(
            //    Vertex(
            //        Vector3f(-3f, 3f, -3f),
            //        Vector3f(0f, 0f, 1f),
            //        Vector2f(0f, 0f),
            //    ),
            //    Vertex(
            //        Vector3f(3f, 3f, -3f),
            //        Vector3f(0f, 0f, 1f),
            //        Vector2f(1f, 0f),
            //    ),
            //    Vertex(
            //        Vector3f(0f, 4f, -3f),
            //        Vector3f(0f, 0f, 1f),
            //        Vector2f(.5f, 1f),
            //    ),
            //    Material(
            //        texture = Texture.fromImageFile("/textures/numbered-checker.png")
            //    ),
            //),
            //Plane(
            //    Vector3f(-.5f, 0f, -3f),
            //    Vector3f(0f, 1f, 0f),
            //    Material(
            //        ColorRGB.GREY,
            //        texture = Texture.fromImageResource("/textures/cement-1.png"),
            //        uvScale = Vector2f(5f, 5f),
            //        wrapMode = WrapMode.MIRROR,
            //    ),
            //    Vector3f(1f, 0f, 0f)
            //)
        ),
    )

    Renderer(scene, width, height).run()
}