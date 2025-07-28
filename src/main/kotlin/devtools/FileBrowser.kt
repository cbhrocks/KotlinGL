package org.kotlingl.devtools

import imgui.ImGui.*
import imgui.ImVec2
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImString
import java.nio.file.Paths


class FileBrowser(
    var path: String,
    val title: String,
    val extensions: List<String>,
): Window() {
    val defaultPath = path
    val currentPath = Paths.get(path)
    val pathInputValue = ImString(path)

    fun upLevel() {

    }

    fun createChildren() {

    }

    override fun update() {
        if (!open.get())
            return

        val width = 840f
        val height = 620f
        val mainViewport = getMainViewport();
        val xPos = (mainViewport.workSizeX - width)/2
        val yPos = (mainViewport.workSizeY - height)/2
        setNextWindowPos(ImVec2(xPos, yPos), ImGuiCond.FirstUseEver);
        setNextWindowSize(ImVec2(width, height), ImGuiCond.FirstUseEver);

        if (begin(title, this.open, ImGuiWindowFlags.MenuBar)) {
            inputText("", pathInputValue)
            sameLine()
            if (button("up")) { upLevel() }

            beginChild("Files")

            endChild()

            button("cancel")
            sameLine()
            button("open")

            end()
        }
    }
}