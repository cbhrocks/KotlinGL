package org.kotlingl.devtools

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import imgui.type.ImBoolean
import imgui.type.ImFloat
import imgui.type.ImInt
import org.joml.Quaternionf
import org.joml.Vector3f
import org.kotlingl.FrameTimer
import org.kotlingl.Input.InputContext
import org.kotlingl.Input.InputEvent
import org.kotlingl.Scene
import org.kotlingl.Settings
import org.kotlingl.math.toFloatArray
import org.kotlingl.model.Model
import org.kotlingl.model.PrimitiveFactory
import org.kotlingl.model.SkeletonNode
import org.kotlingl.utils.checkGLError
import org.lwjgl.glfw.GLFW.GLFW_KEY_A
import org.lwjgl.glfw.GLFW.GLFW_KEY_D
import org.lwjgl.glfw.GLFW.GLFW_KEY_S
import org.lwjgl.glfw.GLFW.GLFW_KEY_W
import org.lwjgl.opengl.ARBFramebufferObject.GL_FRAMEBUFFER
import org.lwjgl.opengl.ARBFramebufferObject.glBindFramebuffer
import org.lwjgl.opengl.GL11

data class FPSWindow(
    var open: ImBoolean,
    var showFPS: ImBoolean = ImBoolean(true)
)

object DevObjects {
    val quads = mutableMapOf<String, Model>()
    val spheres = mutableMapOf<String, Model>()
}

object DevTools {
    lateinit var scene: Scene
    lateinit var frameTimer: FrameTimer
    lateinit var sceneWindow: SceneWindow

    private val activeLayer = ImInt(0)
    private val imguiGlfw = ImGuiImplGlfw()
    private val imguiGl3 = ImGuiImplGl3()
    private val settingsWindowOpen = ImBoolean(false)
    private val newSphereWindowOpen = ImBoolean(false)
    private val statusBar = FPSWindow(ImBoolean(true))
    private val modelWindows = mutableMapOf<String, ModelWindow>()
    private val movementEnabled = ImBoolean(false)
    private val newModelOpen = ImBoolean(false)
    private val newModel2DOpen = ImBoolean(false)

    val inputContext = object : InputContext {
        override fun handleInput(event: InputEvent) {
            val bc = scene.cameraManager.getCamera("background")
            when (event.key) {
                GLFW_KEY_W -> {
                    if (movementEnabled.get()) {
                        bc.position.y += 0.05f; bc.lookAt.y += 0.05f
                        event.consumed = true
                    }
                }
                GLFW_KEY_A -> {
                    if (movementEnabled.get()) {
                        bc.position.x -= 0.05f; bc.lookAt.x -= 0.05f
                        event.consumed = true
                    }
                }
                GLFW_KEY_S -> {
                    if (movementEnabled.get()) {
                        bc.position.y -= 0.05f; bc.lookAt.y -= 0.05f
                        event.consumed = true
                    }
                }
                GLFW_KEY_D -> {
                    if (movementEnabled.get()) {
                        bc.position.x += 0.05f; bc.lookAt.x += 0.05f
                        event.consumed = true
                    }
                }
            }
        }
    }

    fun init(
        windowHandle: Long,
        scene: Scene,
        frameTimer: FrameTimer,
    ) {
        ImGui.createContext()
        val io = ImGui.getIO()
        io.iniFilename = null
        ImGui.styleColorsDark()
        imguiGlfw.init(windowHandle, true)
        imguiGl3.init("#version 330") // your GLSL version

        this.scene = scene
        this.sceneWindow = SceneWindow(scene)
        this.frameTimer = frameTimer
    }

    fun newFrame() {
        imguiGl3.newFrame()
        imguiGlfw.newFrame()
        ImGui.newFrame()
    }

    fun update(timeDelta: Float) {
        newFrame()

        // pack modelWindows
        scene.layers.forEach { name, layer ->
            layer.objects.forEach {
                if (it is Model && !modelWindows.contains(it.name)) {
                    modelWindows.put(it.name, ModelWindow(it))
                }
            }
        }

        createMainMenuBar()
        createSettingsWindow()
        createNewSphereWindow()
        createStatusBar()
        sceneWindow.update()
    }

    fun createMainMenuBar() {
        val mainViewport = ImGui.getMainViewport();
        mainViewport.addFlags(ImGuiWindowFlags.MenuBar)
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("View")) {
                ImGui.menuItem("Settings", null, settingsWindowOpen)
                ImGui.menuItem("Scene Manager", null, sceneWindow.open)
                ImGui.separator()
                if (ImGui.beginMenu("Status Bar")) {
                    ImGui.menuItem("Show", null, statusBar.open)
                    ImGui.menuItem("Show FPS", null, statusBar.showFPS)
                    ImGui.endMenu()

                }
                ImGui.endMenu()
            }
            ImGui.endMainMenuBar()
        }
    }

    fun createSettingsWindow() {
        if (!settingsWindowOpen.get())
            return

        if (ImGui.begin("Settings", settingsWindowOpen)) {
            val windowDimensionsArray = intArrayOf(Settings.windowWidth, Settings.windowHeight)
            val renderDimensionsArray = intArrayOf(Settings.renderWidth, Settings.renderHeight)
            val gameSpeedFloat = ImFloat(Settings.gameSpeed)
            val localDebugRender = ImBoolean(Settings.debugRender)

            ImGui.inputInt2("Window Dimensions", windowDimensionsArray)
            ImGui.inputInt2("Render Dimensions", renderDimensionsArray)
            ImGui.inputFloat("Game Speed", gameSpeedFloat, 0.1f)
            ImGui.separatorText("Dev Settings")
            ImGui.checkbox("Free Movement", movementEnabled)
            ImGui.checkbox("Debug Render", localDebugRender)

            Settings.update {
                windowWidth = windowDimensionsArray[0]
                windowHeight = windowDimensionsArray[1]
                renderWidth = renderDimensionsArray[0]
                renderHeight = renderDimensionsArray[1]
                gameSpeed = gameSpeedFloat.get()
                debugRender = localDebugRender.get()
            }
        }
        ImGui.end()
    }

    fun createNewSphereWindow() {
        if (!newSphereWindowOpen.get()) {
            return
        }

        if (ImGui.begin("New Sphere", newSphereWindowOpen)) {

        }
    }

    fun createStatusBar() {
        if (!statusBar.open.get()) {
            return
        }

        val mainViewport = ImGui.getMainViewport()
        val statusBarHeight = ImGui.getFrameHeight() * 1.4f

        ImGui.setNextWindowPos(ImVec2(
            mainViewport.posX,
            mainViewport.sizeY - statusBarHeight
        ))
        ImGui.setNextWindowSize(ImVec2(
            mainViewport.sizeX,
            statusBarHeight
        ))

        ImGui.begin("Status Bar", null, ImGuiWindowFlags.NoDecoration or ImGuiWindowFlags.NoDocking)

        if (statusBar.showFPS.get()) {
            val fpsDisplay = "FPS: ${frameTimer.fps}"
            ImGui.sameLine(ImGui.getContentRegionMaxX() - ImGui.calcTextSize(fpsDisplay).x)
            ImGui.text(fpsDisplay)
        }

        ImGui.end()
    }

    fun render() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glDisable(GL11.GL_DEPTH_TEST)

        ImGui.render()
        imguiGl3.renderDrawData(ImGui.getDrawData())
        checkGLError("DevTools render")
    }

    fun shutdown() {
        imguiGl3.shutdown()
        imguiGlfw.shutdown()
        ImGui.destroyContext()
    }

    fun createNewQuad() {
        val name = "Quad_" + DevObjects.quads.count() + 1
        val newQuad = PrimitiveFactory.createQuad(name).apply {initGL()}
        DevObjects.quads[name] = newQuad
        scene.layers.getValue(scene.getLayerNames()[activeLayer.get()]).objects.addLast(newQuad)
    }

    fun createNewSphere() {
        val name = "Sphere_" + DevObjects.spheres.count() + 1
        val newSphere = PrimitiveFactory.createSphere(name).apply {initGL()}
        DevObjects.spheres[name] = newSphere
        scene.layers.getValue(scene.getLayerNames()[activeLayer.get()]).objects.addLast(newSphere)
    }
}