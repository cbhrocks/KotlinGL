package org.kotlingl.Input

import org.lwjgl.glfw.GLFW

object InputManager {
    enum class KeyState { UP, PRESSED, HELD, RELEASED }
    val states = Array<KeyState>(GLFW.GLFW_KEY_LAST + 1) { KeyState.UP }
    val keysToReset = mutableListOf<Int>()
    val keysToHold = mutableListOf<Int>()
    val holdTimes = mutableMapOf<Int, Float>()
    private val mouseButtons = BooleanArray(GLFW.GLFW_MOUSE_BUTTON_LAST)

    fun init(window: Long) {
        GLFW.glfwSetKeyCallback(window) { _, key, _, action, _ ->
            if (key in states.indices) {
                val prevState = states[key]
                when (action) {
                    GLFW.GLFW_PRESS -> {
                        states[key] = KeyState.PRESSED
                        keysToHold.add(key)
                    }
                    GLFW.GLFW_RELEASE -> {
                        if (prevState == KeyState.HELD || prevState == KeyState.PRESSED) {
                            states[key] = KeyState.RELEASED
                            keysToReset.add(key)
                        }
                    }
                }
                //states[key] = action != GLFW.GLFW_RELEASE
            }
        }

        GLFW.glfwSetMouseButtonCallback(window) { _, button, action, _ ->
            if (button in mouseButtons.indices) {
                mouseButtons[button] = action != GLFW.GLFW_RELEASE
            }
        }

        // (Optional) Set cursor position, scroll, etc.
    }

    // call after input events are processed
    fun update(timeDelta: Float) {
        keysToReset.forEach {
            if (states[it] == KeyState.RELEASED)
                states[it] = KeyState.UP
        }
        keysToHold.forEach {
            if (states[it] == KeyState.PRESSED)
                states[it] = KeyState.HELD
            holdTimes.merge(it, timeDelta) { key, value -> value + timeDelta }
        }
        keysToReset.clear()
    }

    fun isPressed(key: Int) = states[key] == KeyState.PRESSED
    fun isHeld(key: Int) = states[key] == KeyState.HELD
    fun isPressedOrHeld(key: Int) = states[key] == KeyState.PRESSED || states[key] == KeyState.HELD
    fun isActive(key: Int) = states[key] != KeyState.UP
    fun isMouseButtonDown(button: Int): Boolean = mouseButtons.getOrElse(button) { false }
}