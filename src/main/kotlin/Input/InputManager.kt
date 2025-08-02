package org.kotlingl.Input

import org.lwjgl.glfw.GLFW

object InputManager {
    private val mouseButtons = BooleanArray(GLFW.GLFW_MOUSE_BUTTON_LAST)

    fun init(window: Long) {
        GLFW.glfwSetKeyCallback(window) { _, key, _, action, _ ->
            val keyAction = when (action) {
                GLFW.GLFW_PRESS -> KeyAction.PRESSED
                GLFW.GLFW_RELEASE -> KeyAction.RELEASED
                else -> null
            }
            if (keyAction != null) {
                InputHandler.receiveInput(key, keyAction)
            }
        }

        GLFW.glfwSetMouseButtonCallback(window) { _, button, action, _ ->
            if (button in mouseButtons.indices) {
                mouseButtons[button] = action != GLFW.GLFW_RELEASE
            }
        }

    }
}