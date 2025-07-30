package org.kotlingl.devtools

import imgui.ImGui
import imgui.ImGui.*
import imgui.ImVec2
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImInt
import imgui.type.ImString
import org.kotlingl.Scene
import org.kotlingl.model.Model
import org.kotlingl.model.ModelLoader
import org.kotlingl.model.PrimitiveFactory
import java.nio.file.Paths
import kotlin.io.path.absolute

class SceneWindow(val scene: Scene): Window() {
    private val activeLayerIndex = ImInt(0)
    private val modelWindows = mutableMapOf<String, ModelWindow>()
    private val fileBrowser = FileBrowser(Paths.get("src/main/resources").absolute())
    object newModelModal {
        val title = "New Model"
        val name = ImString()
        val path = ImString()
    }

    fun newModelModal() {

        val center = getMainViewport().getCenter()
        setNextWindowPos(center, ImGuiCond.Appearing, ImVec2(0.5f, 0.5f));

        if (beginPopupModal(newModelModal.title, ImGuiWindowFlags.AlwaysAutoResize))
        {
            inputText("Name", newModelModal.name)
            inputText("Path", newModelModal.path)
            sameLine()
            if (button("browse...")) {
                fileBrowser.open(
                    "Open 3D Model",
                    "*.fbx"
                ) {
                    newModelModal.path.set(it.toString())
                }
            }

            if (button("Cancel", ImVec2(120f, 0f))) {
                closeCurrentPopup();
            }
            sameLine()
            if (button("Add", ImVec2(120f, 0f))) {
                ModelLoader.loadModel(Paths.get(newModelModal.path.get()), "test")
                val model = ModelLoader.createModel("test").apply { initGL() }
                DevTools.scene.layers.getValue(DevTools.scene.getLayerNames()[activeLayerIndex.get()])
                    .objects.addLast(model)
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

            ImGui.combo("Active Layer", activeLayerIndex, scene.layers.keys.toTypedArray())

            val activeLayer = scene.layers.keys.elementAt(activeLayerIndex.get())
            scene.layers.forEach { layer ->
                if (activeLayer == layer.key)
                    pushStyleColor(ImGuiCol.Header, 0f, 0.6f, 0f, 1f)
                if (collapsingHeader("Layer ${layer.key}")) {
                    layer.value.objects.filter {
                        it is Model
                    }.forEach { model ->
                        model as Model
                        text(model.name)
                        sameLine()
                        val modelWindow = modelWindows.getOrPut(model.name) {ModelWindow(model)}
                        val viewButtonLabel = if (modelWindow.open.get()) "Hide" else "View"
                        if (button(viewButtonLabel + "###_${model.name}")) {
                            modelWindow.open.set(!modelWindow.open.get())
                        }
                    }
                }
                if (activeLayer == layer.key)
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
                    if (menuItem("Model 3D...")){
                        openPopup(newModelModal.title)
                    }
                    if (menuItem("Model 2D...")){
                        fileBrowser.open(
                            "Open 2D Model",
                            "*.xml"
                        ) {
                            ModelLoader.loadModelFromSpriteSheetAtlas(it, "test")
                        }
                    }
                    endMenu()
                }
                endMenuBar()
            }
            newModelModal()
        }
        end()
        modelWindows.values.forEach { it.update() }
        fileBrowser.update()
    }

    fun createNewQuad() {
        val name = "Quad_" + DevObjects.quads.count() + 1
        val newQuad = PrimitiveFactory.createQuad(name).apply {initGL()}
        DevObjects.quads[name] = newQuad
        DevTools.scene.layers.getValue(DevTools.scene.getLayerNames()[activeLayerIndex.get()]).objects.addLast(newQuad)
    }

    fun createNewSphere() {
        val name = "Sphere_" + DevObjects.spheres.count() + 1
        val newSphere = PrimitiveFactory.createSphere(name).apply {initGL()}
        DevObjects.spheres[name] = newSphere
        DevTools.scene.layers.getValue(DevTools.scene.getLayerNames()[activeLayerIndex.get()]).objects.addLast(newSphere)
    }
}