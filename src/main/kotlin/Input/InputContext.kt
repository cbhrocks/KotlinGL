package org.kotlingl.Input

import org.lwjgl.glfw.GLFW.*

interface InputContext {
    fun handleInput(event: InputEvent)
}

class MenuInputContext() : InputContext {
    override fun handleInput(event: InputEvent) {
        when (event.action) {
            KeyAction.PRESSED -> when (event.key) {
                GLFW_KEY_W, GLFW_KEY_UP -> {
                    TODO()
                }
                GLFW_KEY_ESCAPE -> {
                    InputHandler.deregisterContext()
                }
            }
            KeyAction.RELEASED -> Unit
        }
    }
}
