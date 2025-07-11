package org.kotlingl

import ShaderProgram
import imgui.ImGui
import org.joml.Quaternionf
import org.joml.Vector3f
import org.kotlingl.lighting.*
import org.kotlingl.model.ModelLoader
import org.kotlingl.FrameTimer
import org.kotlingl.renderer.ModelRenderer
import org.kotlingl.WindowManager
import org.kotlingl.model.PrimitiveFactory
import org.kotlingl.renderer.BackgroundRenderer
import org.kotlingl.renderer.Compositor
import org.kotlingl.renderer.RayTraceRenderer
import org.kotlingl.renderer.RenderPipeline
import org.kotlingl.renderer.UIRenderer
import org.kotlingl.renderer.WorldRenderer
import org.kotlingl.utils.isGLReady
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import kotlin.math.PI

fun main() {
    val ml = ModelLoader()
    //ml.loadModel("/models/spider/spider.obj", "spider")
    //ml.loadModel("/models/box/box.obj", "box")
    ml.loadModel("/models/blocky-characters/FBX format/character-a.fbx", "blocky-character-a")
    // val spider = ml.createModel("spider")
    // spider.transform(
    //     Vector3f(2f, 0f, 0f),
    //     rotation = Quaternionf().rotateY(-PI.toFloat()/2f),
    //     scale = Vector3f(.02f, .02f, .02f)
    // )
    //val box = ml.createModel("box")
    val blockyChar = ml.createModel("blocky-character-a")
    blockyChar.transform(
        rotation = Quaternionf().rotateY(PI.toFloat())//.rotateX(PI.toFloat()/2f),
    )
    blockyChar.skeletonAnimator.currentAnimation = blockyChar.skeleton.animations["walk"]
    //val blockyChar2 = ml.createModel("blocky-character-a")
    //blockyChar2.transform(
    //    Vector3f(2f, 0f, 0f),
    //    rotation = Quaternionf().rotateY(PI.toFloat())//.rotateX(PI.toFloat()/2f),
    //)
    val sphere = PrimitiveFactory.createSphere(
        "sphere"
    )

    val scene = Scene(
        shader = Shader.Builder()
            .addStage(AmbientStage(1f))
            .addStage(DiffuseStage())
            .addStage(SpecularStage())
            .build(),
        cameraManager = CameraManager(
            mutableMapOf("world" to Camera(
                Vector3f(0f, 1f, -4f),
                lookAt = Vector3f(0f, 1f, 0f),
            ), "background" to Camera(
                Vector3f(0f, 1f, -4f),
                lookAt = Vector3f(0f, 1f, 0f),
            ), "ui" to Camera(
                Vector3f(0f, 1f, -4f),
                lookAt = Vector3f(0f, 1f, 0f),
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
        layers = mutableMapOf(
            "background" to Layer(
                "background",
                mutableListOf(
                    // spider,
                    blockyChar,
                    //blockyChar2,
                    // sphere
                )
            )
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

        val timer = FrameTimer()

        val devTools = DevTools(windowManager.window, scene, timer).apply { init() }

        val compositor = Compositor(
            480,
            240,
            windowManager.width,
            windowManager.height
        )
        compositor.initGL()

        windowManager.onResize { width, height ->
            compositor.viewportWidth = width
            compositor.viewportHeight = height
        }

        val backgroundShader = ShaderProgram(
            ShaderProgram.loadShaderSource("/shaders/basic.vert"),
            ShaderProgram.loadShaderSource("/shaders/basic.frag"),
        )

        val renderPipeline = RenderPipeline(
            BackgroundRenderer(backgroundShader),
            RayTraceRenderer(),
            WorldRenderer(),
            UIRenderer(),
            compositor,
            devTools
        )

        while (!windowManager.shouldClose()) {
            timer.update()
            val dt = timer.deltaTime

            devTools.update(dt)
            scene.update(dt)

            renderPipeline.render(
                scene,
                scene.cameraManager,
            )

            windowManager.pollEvents()
            windowManager.swapBuffers()
        }
        devTools.shutdown()
    }

    // Terminate GLFW and free the error callback
    GLFW.glfwTerminate()
    GLFW.glfwSetErrorCallback(null)?.free()
}