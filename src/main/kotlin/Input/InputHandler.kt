package org.kotlingl.Input

import org.lwjgl.glfw.GLFW.GLFW_KEY_LAST

data class InputEvent(
    val key: Int,
    var consumed: Boolean = false
)

data class RegisteredContext(
    val key: String,
    val context: InputContext,
    val priority: Int,
    val insertionIndex: Int
)

object InputHandler {
    private val contexts = mutableListOf<RegisteredContext>()
    private var insertionIndex = 0

    fun registerContext(key: String, context: InputContext, priority: Int = 0) {
        contexts.add(RegisteredContext(
            key, context, priority, insertionIndex++
        ))
    }

    fun deregisterContext(key: String? = null) {
        contexts.removeIf { it.key == key }
    }

    fun clearContexts() {
        contexts.clear()
    }

    fun update() {
        val sortedContexts = contexts.sortedWith(compareByDescending<RegisteredContext> { it.priority }.thenBy { it.insertionIndex })

        for (key in 0..GLFW_KEY_LAST) {
            // if the key has had a state change or is being held
            if (!InputManager.isActive(key)) {
                continue
            }

            val event = InputEvent(key)
            sortedContexts.forEach {
                if (event.consumed)
                    return@forEach

                it.context.handleInput(event)
            }
        }
    }
}