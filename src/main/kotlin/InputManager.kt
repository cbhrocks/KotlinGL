package org.kotlingl

import org.lwjgl.glfw.GLFW

object InputManager {
    private val keys = BooleanArray(GLFW.GLFW_KEY_LAST)
    private val mouseButtons = BooleanArray(GLFW.GLFW_MOUSE_BUTTON_LAST)

    fun init(window: Long) {
        GLFW.glfwSetKeyCallback(window) { _, key, _, action, _ ->
            if (key in keys.indices) {
                keys[key] = action != GLFW.GLFW_RELEASE
            }
        }

        GLFW.glfwSetMouseButtonCallback(window) { _, button, action, _ ->
            if (button in mouseButtons.indices) {
                mouseButtons[button] = action != GLFW.GLFW_RELEASE
            }
        }

        // (Optional) Set cursor position, scroll, etc.
    }

    fun isKeyPressed(key: Int): Boolean = keys.getOrElse(key) { false }
    fun isMouseButtonDown(button: Int): Boolean = mouseButtons.getOrElse(button) { false }
}