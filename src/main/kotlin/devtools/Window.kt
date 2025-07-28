package org.kotlingl.devtools

import imgui.ImVec2
import imgui.type.ImBoolean

abstract class Window(val windowSize: ImVec2 = ImVec2(480f, 240f)) {
    val open = ImBoolean(false)

    protected fun close() {
        open.set(false)
    }

    protected open fun open() {
        open.set(true)
    }

    abstract fun update()
}