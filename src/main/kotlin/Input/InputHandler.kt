package org.kotlingl.Input

data class InputEvent(
    val key: Int,
    val action: KeyAction,
    var consumed: Boolean = false
)

data class RegisteredContext(
    val key: String,
    val context: InputContext,
    val priority: Int,
    val insertionIndex: Int
)

enum class KeyAction {
    PRESSED,
    RELEASED
}

object InputHandler {
    val eventQueue = ArrayDeque<InputEvent>()
    val holdTimes = mutableMapOf<Int, Float>()

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

    fun receiveInput(key: Int, action: KeyAction) {
        eventQueue.addLast(InputEvent(
            key,
            action,
        ))
    }

    private fun updateHoldTimes(key: Int, timeDelta: Float) {
        holdTimes.merge(key, timeDelta) { key, value -> value + timeDelta }
    }

    fun getHoldTime(key: Int): Float {
        return holdTimes.getOrDefault(key, 0f)
    }

    fun update(timeDelta: Float) {
        val sortedContexts = contexts.sortedWith(compareByDescending<RegisteredContext> { it.priority }.thenBy { it.insertionIndex })

        while (eventQueue.isNotEmpty()) {
            val event = eventQueue.removeFirst()

            updateHoldTimes(event.key, timeDelta)

            sortedContexts.forEach {
                if (event.consumed)
                    return@forEach

                it.context.handleInput(event)
            }
        }
    }
}