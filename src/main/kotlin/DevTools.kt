package org.kotlingl

import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import org.kotlingl.renderer.Framebuffer

class DevTools(
    windowHandle: Long,
    val scene: Scene
) {
    private val imguiGlfw = ImGuiImplGlfw()
    private val imguiGl3 = ImGuiImplGl3()

    init {
        ImGui.createContext()
        val io = ImGui.getIO()
        io.iniFilename = null
        ImGui.styleColorsDark()
        imguiGlfw.init(windowHandle, true)
        imguiGl3.init("#version 330") // your GLSL version
    }

    fun newFrame() {
        imguiGlfw.newFrame()
        ImGui.newFrame()
    }

    fun update(timeDelta: Float) {
        newFrame()

        ImGui.begin("Debug Tools")
        ImGui.text("Hello, Debug World!")
        ImGui.end()
    }

    fun render() {
        ImGui.render()
        imguiGl3.renderDrawData(ImGui.getDrawData())
    }

    fun shutdown() {
        imguiGl3.dispose()
        imguiGlfw.dispose()
        ImGui.destroyContext()
    }
}