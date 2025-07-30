package org.kotlingl.devtools

import imgui.ImGui.getMainViewport
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImVec2
import imgui.flag.ImGuiCond

object Utils {
    fun centerNextWindow(dimensions: ImVec2) {
        val mainViewport = getMainViewport();

        val xPos = (mainViewport.workSizeX - dimensions.x)/2
        val yPos = (mainViewport.workSizeY - dimensions.y)/2

        setNextWindowPos(xPos, yPos, ImGuiCond.FirstUseEver);
        setNextWindowSize(dimensions, ImGuiCond.FirstUseEver);
    }
}