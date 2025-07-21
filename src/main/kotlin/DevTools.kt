package org.kotlingl

import imgui.ImColor
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
import imgui.type.ImString
import org.joml.Quaternionf
import org.joml.Vector3f
import org.kotlingl.Input.InputContext
import org.kotlingl.Input.InputEvent
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

data class ModelWindow (
    var open: ImBoolean,
    val model: Model,
    val rotationTransform: FloatArray = floatArrayOf()
)

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

    private val activeLayer = ImInt(0)
    private val imguiGlfw = ImGuiImplGlfw()
    private val imguiGl3 = ImGuiImplGl3()
    private val settingsWindowOpen = ImBoolean(false)
    private val sceneWindowOpen = ImBoolean(false)
    private val newSphereWindowOpen = ImBoolean(false)
    private val statusBar = FPSWindow(ImBoolean(true))
    private val modelWindows = mutableMapOf<String, ModelWindow>()
    private val movementEnabled = ImBoolean(false)

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
                    val rotationArray = it.rotation
                        .getEulerAnglesXYZ(Vector3f())
                        .mul(Math.toDegrees(1.0).toFloat())
                        .toFloatArray()

                    modelWindows.put(it.name, ModelWindow(
                        ImBoolean(false),
                        it,
                        rotationArray
                    ))
                }
            }
        }

        createMainMenuBar()
        createSettingsWindow()
        createSceneManagerWindow()
        createNewSphereWindow()
        createModelWindows()
        createStatusBar()
    }

    fun createMainMenuBar() {
        val mainViewport = ImGui.getMainViewport();
        mainViewport.addFlags(ImGuiWindowFlags.MenuBar)
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("View")) {
                ImGui.menuItem("Settings", null, settingsWindowOpen)
                ImGui.menuItem("Scene Manager", null, sceneWindowOpen)
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

    fun createSceneManagerWindow() {
        if (!sceneWindowOpen.get())
            return

        val mainViewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(ImVec2(mainViewport.workPosX + 650, mainViewport.workPosY + 20), ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(ImVec2(550f, 680f), ImGuiCond.FirstUseEver);

        if (ImGui.begin("Scene Objects", sceneWindowOpen, ImGuiWindowFlags.MenuBar)) {

            ImGui.combo("Active Layer", activeLayer, scene.layers.keys.toTypedArray())

            scene.layers.forEach { layer ->
                if (scene.getLayerNames()[activeLayer.get()]!! == layer.key)
                    ImGui.pushStyleColor(ImGuiCol.Header, 0f, 0.6f, 0f, 1f)
                if (ImGui.collapsingHeader("Layer ${layer.key}")) {
                    layer.value.objects.filter {
                        it is Model
                    }.forEach { model ->
                        model as Model
                        ImGui.text(model.name)
                        ImGui.sameLine()
                        val modelWindow = modelWindows.getValue(model.name)
                        val viewButtonLabel = if (modelWindow.open.get()) "Hide" else "View"
                        if (ImGui.button(viewButtonLabel + "###_${model.name}")) {
                            modelWindow.open.set(!modelWindow.open.get())
                        }
                    }
                }
                if (scene.layers.keys.toTypedArray()[activeLayer.get()]!! == layer.key)
                    ImGui.popStyleColor()
            }

            if (ImGui.beginMenuBar()) {
                if (ImGui.beginMenu("Insert")) {
                    if (ImGui.beginMenu("Primitive")) {
                        if (ImGui.menuItem("Sphere")) {
                            createNewSphere()
                        }
                        if (ImGui.menuItem("Quad")) {
                            createNewQuad()
                        }
                        ImGui.endMenu()
                    }
                    ImGui.endMenu()
                }
                ImGui.endMenuBar()
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

    fun createModelWindows() {
        val mainViewport = ImGui.getMainViewport();

        var count = 1;
        modelWindows.filter{ it.value.open.get() }.forEach {
            ImGui.setNextWindowPos(ImVec2(mainViewport.workPosX + 650, mainViewport.workPosY + 20), ImGuiCond.FirstUseEver);
            ImGui.setNextWindowSize(ImVec2(200f, 250f), ImGuiCond.FirstUseEver);
            createModelWindow(it.key, it.value)
            count++
        }
    }

    fun createModelWindow(name: String, modelData: ModelWindow) {
        if (ImGui.beginPopupContextItem()) {
            if (ImGui.menuItem("Close")) {
                this.modelWindows.remove(name)
            }
        }

        if (ImGui.begin(name, modelData.open)) {
            val model = modelData.model
            val posArray = model.position.toFloatArray()
            val scaleArray = model.scale.toFloatArray()
            val animationSpeedArray = floatArrayOf(model.skeletonAnimator.animationSpeed)

            ImGui.separatorText("Transforms")
            ImGui.dragFloat3("Position", posArray)
            ImGui.dragFloat3("Rotation", modelData.rotationTransform)
            ImGui.dragFloat3("Scale", scaleArray)

            ImGui.separatorText("Animations")
            ImGui.dragFloat("AnimationSpeed", animationSpeedArray, 0.1f, -5.0f, 5.0f)
            ImGui.beginChild("Animations", ImVec2(ImGui.getContentRegionAvailX(), 260f))
            model.skeleton.animations.forEach {
                ImGui.text(it.value.name)
                val isPlaying = model.skeletonAnimator.currentAnimation == it.value
                val playSize = ImGui.calcTextSizeX("Play") + ImGui.getFrameHeight()
                val loopSize = ImGui.calcTextSizeX("Loop") + ImGui.getFrameHeight()
                ImGui.sameLine(ImGui.getContentRegionMaxX() - playSize * 2)
                if (ImGui.button(if (isPlaying) "Stop" else "Play" + "###play_${it.key}")) {
                    model.skeletonAnimator.currentAnimation = if (isPlaying) null else it.value
                }
                ImGui.sameLine(ImGui.getContentRegionMaxX() - loopSize)
                if (ImGui.checkbox("Loop###loop_${it.key}", it.value.isLoop)) {
                    it.value.isLoop = !it.value.isLoop
                }
            }
            ImGui.endChild()

            ImGui.separatorText("Skeleton")
            ImGui.beginChild("Nodes", ImVec2(ImGui.getContentRegionAvailX(), 260f))
            createSkeletonNode(model, model.skeleton.nodeMap.getValue(model.skeleton.rootId))
            ImGui.endChild()


            // sync values
            modelData.model.transform(
                Vector3f(posArray),
                Quaternionf().identity().rotateXYZ(
                    Math.toRadians(modelData.rotationTransform[0].toDouble()).toFloat(),
                    Math.toRadians(modelData.rotationTransform[1].toDouble()).toFloat(),
                    Math.toRadians(modelData.rotationTransform[2].toDouble()).toFloat()
                ),
                Vector3f(scaleArray)
            )
            modelData.model.skeletonAnimator.animationSpeed = animationSpeedArray[0]

        }
        ImGui.end()
    }

    fun createSkeletonNode(model: Model, currentNode: SkeletonNode) {
        if (ImGui.treeNodeEx(currentNode.name, ImGuiTreeNodeFlags.DefaultOpen)) {
            val meshIndices = model.nodeIdToMeshIndices[currentNode.id]
            meshIndices?.forEach {
                val mesh = model.meshes[it]
                val material = model.getMeshMaterial(it)
                if (ImGui.treeNodeEx("Mesh: ${mesh.name}")) {
                    ImGui.text("Material: ${material.name}")
                    ImGui.treePop()
                }
            }
            currentNode.childIds.forEach {
                createSkeletonNode(model, model.skeleton.nodeMap.getValue(it))
            }
            ImGui.treePop()
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