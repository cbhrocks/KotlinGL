package org.kotlingl

import org.joml.Quaternionf
import org.joml.Vector3f
import org.kotlingl.lighting.*
import org.kotlingl.model.ModelLoader
import org.kotlingl.FrameTimer
import org.kotlingl.renderer.ModelRenderer
import org.kotlingl.WindowManager
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import kotlin.math.PI

fun main() {
    val width = 480
    //val width = 1920
    val height = 240
    //val height = 1080
    val ml = ModelLoader()
    ml.loadModel("/models/spider/spider.obj", "spider")
    //ml.loadModel("/models/box/box.obj", "box")
    ml.loadModel("/models/blocky-characters/FBX format/character-a.fbx", "blocky-character-a")
    ml.loadModel("/models/blocky-characters/OBJ format/character-a.obj", "blocky-character-a-obj")
    val spider = ml.createModel("spider")
    spider.transform(
        Vector3f(2f, 0f, 0f),
        rotation = Quaternionf().rotateY(-PI.toFloat()/2f),
        scale = Vector3f(.02f, .02f, .02f)
    )
    //val box = ml.createModel("box")
    val blockyChar = ml.createModel("blocky-character-a")
    blockyChar.transform(
        rotation = Quaternionf().rotateY(PI.toFloat())//.rotateX(PI.toFloat()/2f),
    )
    blockyChar.skeletonAnimator.currentAnimation = blockyChar.skeleton.animations["walk"]
    val blockyChar2 = ml.createModel("blocky-character-a")
    blockyChar2.transform(
        Vector3f(2f, 0f, 0f),
        rotation = Quaternionf().rotateY(PI.toFloat())//.rotateX(PI.toFloat()/2f),
    )
    //blockyCharOBJ.bvhTree.refit()

    val scene = Scene(
        shader = Shader.Builder()
            .addStage(AmbientStage(1f))
            .addStage(DiffuseStage())
            .addStage(SpecularStage())
            .build(),
        cameraManager = CameraManager(
            mutableMapOf("world" to Camera(
                // Vector3f(-1f, 1f, 120f),
                // lookAt = Vector3f(0f, 0f, 0f),
                //Vector3f(-0f, 50f, 200f),
                Vector3f(0f, 1f, -4f),
                lookAt = Vector3f(0f, 1f, 0f),
                resX = width,
                resY = height,
            ))
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
        sceneObjects = mutableListOf(
            spider,
            //box,
            blockyChar,
            blockyChar2
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

    // Setup an error callback. The default implementation
    // will print the error message in System.err.
    GLFWErrorCallback.createPrint(System.err).set()

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

    WindowManager().use { windowManager ->
        windowManager.initWindow()
        println("Hello LWJGL " + Version.getVersion() + "!")
        InputManager.init(windowManager.window)

        scene.initGL()
        val mr = ModelRenderer(width, height)

        val timer = FrameTimer()
        while (!windowManager.shouldClose()) {
            timer.update()
            val dt = timer.deltaTime
            println("New Frame: ${timer.totalTime} (${timer.deltaTime})")

            scene.update(dt)

            mr.traceRays(scene)

            windowManager.pollEvents()
            windowManager.swapBuffers()
        }
    }

    // Terminate GLFW and free the error callback
    GLFW.glfwTerminate()
    GLFW.glfwSetErrorCallback(null)?.free()
}