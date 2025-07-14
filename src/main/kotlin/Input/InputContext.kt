package org.kotlingl.Input

import org.lwjgl.glfw.GLFW.*

interface InputContext {
    fun handleInput(event: InputEvent)
}

class MenuInputContext() : InputContext {
    override fun handleInput(event: InputEvent) {
        if (InputManager.isPressed(GLFW_KEY_UP)) {
            //menu.navigateUp()
        }

        if (InputManager.isPressed(GLFW_KEY_ESCAPE)) {
            InputHandler.deregisterContext() // back to game
        }
    }
}
