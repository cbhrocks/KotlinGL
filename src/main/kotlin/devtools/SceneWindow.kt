package org.kotlingl.devtools

import imgui.ImGui
import imgui.ImGui.*
import imgui.ImVec2
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImInt
import imgui.type.ImString
import org.kotlingl.Layer
import org.kotlingl.Scene
import org.kotlingl.model.Model
import org.kotlingl.model.ModelLoader
import org.kotlingl.model.PrimitiveFactory
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.extension

class SceneWindow(val scene: Scene): Window() {
    private val activeLayerIndex = ImInt(0)
    private val modelWindows = mutableMapOf<String, ModelWindow>()
    private val fileBrowser = FileBrowser(Paths.get("src/main/resources").absolute())
    object newModelModal {
        const val TITLE = "New Model"
        var open = false
        val name = ImString()
        val path = ImString("", 256)
    }

    val activeLayerName: String
        get() {
            return scene.layers.keys.elementAt(activeLayerIndex.get())
        }
    val activeLayer: Layer
        get() {
            return scene.layers.values.elementAt(activeLayerIndex.get())
        }

    fun createNewModelModal() {

        val center = getMainViewport().getCenter()
        setNextWindowPos(center, ImGuiCond.Appearing, ImVec2(0.5f, 0.5f));

        if (beginPopupModal(newModelModal.TITLE, ImGuiWindowFlags.AlwaysAutoResize))
        {
            inputText("Name", newModelModal.name)
            inputText("Path", newModelModal.path)
            sameLine()
            if (button("browse...")) {
                fileBrowser.open(
                    "Open 3D Model",
                    // "*.fbx"
                ) {
                    newModelModal.path.set(it.toString())
                }
            }

            fileBrowser.update()

            if (button("Cancel", ImVec2(120f, 0f))) {
                closeCurrentPopup();
            }
            sameLine()
            if (button("Add", ImVec2(120f, 0f))) {
                val path = Paths.get(newModelModal.path.get())
                val name = newModelModal.name.get()
                val model = when (path.extension) {
                    "xml" -> {
                        ModelLoader.loadModelFromSpriteSheetAtlas(path, name)
                        ModelLoader.createModel2D(name).apply { initGL() }
                    }
                    else -> {
                        ModelLoader.loadModel(path, name)
                        ModelLoader.createModel(name).apply { initGL() }
                    }
                }
                scene.addObject(activeLayerName, model)
                closeCurrentPopup();
            }
            endPopup()
        }
    }

    override fun update() {
        if (!open.get())
            return

        val mainViewport = getMainViewport();
        setNextWindowPos(ImVec2(mainViewport.workPosX + 650, mainViewport.workPosY + 20), ImGuiCond.FirstUseEver);
        setNextWindowSize(ImVec2(550f, 680f), ImGuiCond.FirstUseEver);

        if (begin("Scene Objects", open, ImGuiWindowFlags.MenuBar)) {

            combo("Active Layer", activeLayerIndex, scene.layers.keys.toTypedArray())

            scene.layers.forEach { layer ->
                if (activeLayerName == layer.key)
                    pushStyleColor(ImGuiCol.Header, 0f, 0.6f, 0f, 1f)
                if (collapsingHeader("Layer ${layer.key}")) {
                    layer.value.objects.forEach {
                        it.model as Model
                        text(it.model.name)
                        sameLine()
                        val modelWindow = modelWindows.getOrPut(it.model.name) {ModelWindow(it.model)}
                        val viewButtonLabel = if (modelWindow.open.get()) "Hide" else "View"
                        if (button(viewButtonLabel + "###_${it.model.name}")) {
                            modelWindow.open.set(!modelWindow.open.get())
                        }
                    }
                }
                if (activeLayerName == layer.key)
                    popStyleColor()
            }

            if (beginMenuBar()) {
                if (beginMenu("Insert")) {
                    if (menuItem("Sphere")) {
                        createNewSphere()
                    }
                    if (menuItem("Quad")) {
                        createNewQuad()
                    }
                    if (menuItem("Model...")){
                        newModelModal.open = true
                    }
                    endMenu()
                }
                endMenuBar()
            }
            if (newModelModal.open) {
                openPopup(newModelModal.TITLE)
                newModelModal.open = false
            }
            createNewModelModal()
        }
        end()
        modelWindows.values.forEach { it.update() }
        // fileBrowser.update()
    }

    fun createNewQuad() {
        val name = "Quad_" + DevObjects.quads.count() + 1
        val newQuad = PrimitiveFactory.createQuad(name).apply {initGL()}
        DevObjects.quads[name] = newQuad
        scene.addObject(activeLayerName, newQuad)
    }

    fun createNewSphere() {
        val name = "Sphere_" + DevObjects.spheres.count() + 1
        val newSphere = PrimitiveFactory.createSphere(name).apply {initGL()}
        DevObjects.spheres[name] = newSphere
        scene.addObject(activeLayerName, newSphere)
    }
}