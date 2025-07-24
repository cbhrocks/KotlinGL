package org.kotlingl.devtools

import imgui.type.ImBoolean

abstract class Window {
    val open = ImBoolean(false)

    protected fun close() {
        open.set(false)
    }

    protected fun open() {
        open.set(true)
    }

    abstract fun update()
}