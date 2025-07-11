package org.kotlingl

import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import org.kotlingl.model.Model
import org.kotlingl.renderer.Framebuffer
import org.kotlingl.utils.checkGLError
import org.lwjgl.opengl.ARBFramebufferObject.GL_FRAMEBUFFER
import org.lwjgl.opengl.ARBFramebufferObject.glBindFramebuffer
import org.lwjgl.opengl.GL11

class DevTools(
    val windowHandle: Long,
    val scene: Scene
) {
    private val imguiGlfw = ImGuiImplGlfw()
    private val imguiGl3 = ImGuiImplGl3()

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

        ImGui.begin("Debug Tools")
        ImGui.text("Hello, Debug World!")
        ImGui.end()

        ImGui.begin("Scene Objects")
        scene.layers.forEach {
            if (ImGui.collapsingHeader("Layer ${it.key}")) {
                for (item in it.value.objects) {
                    when (item) {
                        is Model ->
                            ImGui.text("item: ${item.name}")
                    }
                }
            }
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