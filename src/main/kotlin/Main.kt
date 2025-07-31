package org.kotlingl

import ShaderProgram
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.kotlingl.Collider.BoxCollider
import org.kotlingl.Input.InputContext
import org.kotlingl.Input.InputEvent
import org.kotlingl.Input.InputHandler
import org.kotlingl.Input.InputManager
import org.kotlingl.Input.MenuInputContext
import org.kotlingl.devtools.DevTools
import org.kotlingl.lighting.*
import org.kotlingl.model.ModelLoader
import org.kotlingl.model.PrimitiveFactory
import org.kotlingl.renderer.BackgroundRenderer
import org.kotlingl.renderer.Compositor
import org.kotlingl.renderer.DebugRenderer
import org.kotlingl.renderer.RayTraceRenderer
import org.kotlingl.renderer.RenderPipeline
import org.kotlingl.renderer.UIRenderer
import org.kotlingl.renderer.WorldRenderer
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.GLFW_KEY_A
import org.lwjgl.glfw.GLFW.GLFW_KEY_D
import org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
import org.lwjgl.glfw.GLFW.GLFW_KEY_GRAVE_ACCENT
import org.lwjgl.glfw.GLFW.GLFW_KEY_S
import org.lwjgl.glfw.GLFW.GLFW_KEY_W
import org.lwjgl.glfw.GLFWErrorCallback
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.math.PI

object Settings {
    var windowWidth: Int = 1280
    var windowHeight: Int = 720
    var renderWidth: Int = 480
    var renderHeight: Int = 240
    var gameSpeed: Float = 1.0f
    var devMode: Boolean = false
    var debugRender: Boolean = false

    val listeners = mutableListOf<(settings: Settings) -> Unit>()

    fun addListener(listener: (settings: Settings) -> Unit) {
        listeners.add(listener)
    }

    fun update(action: Settings.() -> Unit): Unit {
        this.action()
        listeners.forEach {it(this)}
    }
}

object GameInputContext : InputContext {
    override fun handleInput(event: InputEvent) {
        when (event.key) {
            GLFW_KEY_W -> {
                if (InputManager.isPressedOrHeld(event.key)) { }
                event.consumed = true
            }
            GLFW_KEY_A -> {
                if (InputManager.isPressedOrHeld(event.key)) { }
                event.consumed = true
            }
            GLFW_KEY_S -> {
                if (InputManager.isPressedOrHeld(event.key)) { }
                event.consumed = true
            }
            GLFW_KEY_D -> {
                if (InputManager.isPressedOrHeld(event.key)) { }
                event.consumed = true
            }
            GLFW_KEY_GRAVE_ACCENT -> {
                if (InputManager.isPressed(event.key)) {
                    if (!Settings.devMode) {
                        InputHandler.registerContext("DevTools", DevTools.inputContext, 10)
                    }
                    else {
                        InputHandler.deregisterContext("DevTools")
                    }
                    Settings.update {
                        devMode = !Settings.devMode
                    }
                }
            }
        }

        if (InputManager.isPressed(GLFW_KEY_ESCAPE)) {
            InputHandler.registerContext("MainMenu", MenuInputContext())
        }
    }
}

fun main() {
    //ml.loadModel("/models/spider/spider.obj", "spider")
    //ml.loadModel("/models/box/box.obj", "box")
    ModelLoader.loadModel(Paths.get("/models/blocky-characters/FBX format/character-a.fbx"), "blocky-character-a")
    ModelLoader.loadModelFromSpriteSheetAtlas(Paths.get("/sprites/kenney-platformer-pack/Spritesheets/spritesheet-characters-default.xml"), "test-sprite")
    // val spider = ml.createModel("spider")
    // spider.transform(
    //     Vector3f(2f, 0f, 0f),
    //     rotation = Quaternionf().rotateY(-PI.toFloat()/2f),
    //     scale = Vector3f(.02f, .02f, .02f)
    // )
    //val box = ml.createModel("box")
    val blockyChar = ModelLoader.createModel("blocky-character-a")
    blockyChar.transform(
        rotation = Quaternionf().rotateY(PI.toFloat())//.rotateX(PI.toFloat()/2f),
    )
    blockyChar.skeletonAnimator.currentAnimation = blockyChar.skeleton.animations["walk"]
    val testSprite = ModelLoader.createModel2D("test-sprite")
    testSprite.textureAnimator.playAnimation("quad", "character_beige_walk")
    //val blockyChar2 = ml.createModel("blocky-character-a")
    //blockyChar2.transform(
    //    Vector3f(2f, 0f, 0f),
    //    rotation = Quaternionf().rotateY(PI.toFloat())//.rotateX(PI.toFloat()/2f),
    //)
    val sphere = PrimitiveFactory.createSphere(
        "sphere"
    )
    val quad = PrimitiveFactory.createQuad(
        "quad"
    )

    val scene = Scene(
        shader = Shader.Builder()
            .addStage(AmbientStage(1f))
            .addStage(DiffuseStage())
            .addStage(SpecularStage())
            .build(),
        cameraManager = CameraManager(
            mutableMapOf("world" to Camera(
                Vector3f(0f, 1f, 4f),
                lookAt = Vector3f(0f, 1f, 0f),
            ), "background" to Camera(
                Vector3f(0f, 1f, 4f),
                lookAt = Vector3f(0f, 1f, 0f),
            ), "ui" to Camera(
                Vector3f(0f, 1f, 4f),
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
        layers = sortedMapOf(
            "ui" to Layer(
                "ui",
                mutableListOf()
            ),
            "background" to Layer(
                "background",
                mutableListOf(
                    // spider,
                    LayerObject(blockyChar),
                    LayerObject(testSprite)
                    //blockyChar2,
                    // sphere
                    // quad
                )
            ),
            "world" to Layer(
                "world",
                mutableListOf()
            ),
            "foreground" to Layer(
                "foreground",
                mutableListOf()
            ),
        ),
        colliders = mutableListOf(
            BoxCollider(
                blockyChar.sharedMatrix,
                Vector2f(0.75f, 1.5f),
                CollisionLayers.PLAYER,
                CollisionLayers.ENEMY or CollisionLayers.PROJECTILE or CollisionLayers.ENVIRONMENT,
                Vector2f(0.0f, 1.5f)
            )
        )
    )

    // Setup an error callback. The default implementation
    // will print the error message in System.err.
    GLFWErrorCallback.createPrint(System.err).set()

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

    WindowManager().use { windowManager ->
        windowManager.initWindow(Settings.windowWidth, Settings.windowHeight)
        println("Hello LWJGL " + Version.getVersion() + "!")
        InputManager.init(windowManager.window)
        InputHandler.registerContext("GameInput", GameInputContext)

        scene.initGL()

        val timer = FrameTimer()

        DevTools.init(windowManager.window, scene, timer)

        val compositor = Compositor(
            Settings.renderWidth,
            Settings.renderHeight,
            Settings.windowWidth,
            Settings.windowHeight,
        )
        compositor.initGL()

        Settings.addListener { it ->
            compositor.viewportWidth = it.windowWidth
            compositor.viewportHeight = it.windowHeight
            compositor.resize(it.renderWidth, it.renderHeight)
        }

        Settings.addListener { it ->
            timer.updateSpeed = it.gameSpeed
        }

        val backgroundShader = ShaderProgram(
            ShaderProgram.loadShaderSource("/shaders/basic.vert"),
            ShaderProgram.loadShaderSource("/shaders/basic.frag"),
        )

        val debugShader = ShaderProgram(
            ShaderProgram.loadShaderSource("/shaders/debug.vert"),
            ShaderProgram.loadShaderSource("/shaders/debug.frag")
        )

        val renderPipeline = RenderPipeline(
            BackgroundRenderer(backgroundShader),
            RayTraceRenderer(),
            WorldRenderer(),
            UIRenderer(),
            compositor,
            DebugRenderer(debugShader).apply { initGL() }
        )

        while (!windowManager.shouldClose()) {
            // prerender update
            timer.update()
            val dt = timer.deltaTime
            InputHandler.update()
            InputManager.update(dt)
            scene.update(dt)

            if (Settings.devMode) {
                DevTools.update(dt)
            }

            //render
            renderPipeline.render(
                scene,
                scene.cameraManager,
            )

            if(Settings.devMode) {
                DevTools.render()
            }

            windowManager.pollEvents()
            windowManager.swapBuffers()
        }
        DevTools.shutdown()
    }

    // Terminate GLFW and free the error callback
    GLFW.glfwTerminate()
    GLFW.glfwSetErrorCallback(null)?.free()
}