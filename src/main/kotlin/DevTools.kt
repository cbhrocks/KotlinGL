package org.kotlingl

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import imgui.type.ImBoolean
import org.joml.Quaternionf
import org.joml.Vector3f
import org.kotlingl.math.toFloatArray
import org.kotlingl.model.Model
import org.kotlingl.model.SkeletonNode
import org.kotlingl.utils.checkGLError
import org.lwjgl.opengl.ARBFramebufferObject.GL_FRAMEBUFFER
import org.lwjgl.opengl.ARBFramebufferObject.glBindFramebuffer
import org.lwjgl.opengl.GL11

data class ModelWindow (
    var open: ImBoolean,
    val model: Model,
    val rotationTransform: FloatArray = floatArrayOf()
)

data class SceneWindow(
    var open: ImBoolean,
    val model: Scene
)

data class FPSWindow(
    var open: ImBoolean,
    var frameTimer: FrameTimer,
    var showFPS: ImBoolean = ImBoolean(true)
)

class DevTools(
    val windowHandle: Long,
    val scene: Scene,
    val frameTimer: FrameTimer,
) {
    private val imguiGlfw = ImGuiImplGlfw()
    private val imguiGl3 = ImGuiImplGl3()
    private val sceneWindow = SceneWindow(ImBoolean(false), scene)
    private val statusBar = FPSWindow(ImBoolean(true), frameTimer)
    private val modelWindows = mutableMapOf<String, ModelWindow>()

    fun init() {
        ImGui.createContext()
        val io = ImGui.getIO()
        io.iniFilename = null
        ImGui.styleColorsDark()
        imguiGlfw.init(windowHandle, true)
        imguiGl3.init("#version 330") // your GLSL version
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
        createSceneManagerWindow()
        createModelWindows()
        createStatusBar()
    }

    fun createMainMenuBar() {
        val mainViewport = ImGui.getMainViewport();
        mainViewport.addFlags(ImGuiWindowFlags.MenuBar)
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("View")) {
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

    fun createSceneManagerWindow() {
        if (!sceneWindow.open.get())
            return

        val mainViewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(ImVec2(mainViewport.workPosX + 650, mainViewport.workPosY + 20), ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(ImVec2(550f, 680f), ImGuiCond.FirstUseEver);

        if (ImGui.begin("Scene Objects", sceneWindow.open)) {
            scene.layers.forEach { layer ->
                if (ImGui.collapsingHeader("Layer ${layer.key}")) {
                    layer.value.objects.filter {
                        it is Model
                    }.forEach { model ->
                        model as Model
                        ImGui.text(model.name)
                        ImGui.sameLine()
                        val modelWindow = modelWindows.getValue(model.name)
                        val viewButtonLabel = if (modelWindow.open.get()) "Hide" else "View"
                        if (ImGui.button(viewButtonLabel)) {
                            modelWindow.open.set(!modelWindow.open.get())
                        }
                    }
                }
            }
        }
        ImGui.end()
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
        ImGui.begin(name, modelData.open)
        if (ImGui.beginPopupContextItem()) {
            if (ImGui.menuItem("Close")) {
                this.modelWindows.remove(name)
            }
        }

        val model = modelData.model

        val posArray = model.position.toFloatArray()
        val scaleArray = model.scale.toFloatArray()
        ImGui.separatorText("Transforms")
        ImGui.dragFloat3("Position", posArray)
        ImGui.dragFloat3("Rotation", modelData.rotationTransform)
        ImGui.dragFloat3("Scale", scaleArray)

        ImGui.separatorText("Animations")
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

        ImGui.end()
    }

    fun createSkeletonNode(model: Model, currentNode: SkeletonNode) {
        if (ImGui.treeNodeEx(currentNode.name, ImGuiTreeNodeFlags.DefaultOpen)) {
            val meshIndices = model.nodeIdToMeshIndices[currentNode.id]
            meshIndices?.forEach {
                val mesh = model.meshes[it]
                if (ImGui.treeNodeEx("Mesh: ${mesh.name}")) {
                    ImGui.text("Material: ${mesh.material.name}")
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
}