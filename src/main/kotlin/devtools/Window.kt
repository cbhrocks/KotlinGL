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

abstract class Dialog(
    windowSize: ImVec2,
    val onSubmit: () -> Unit,
    val onCancel: () -> Unit
): Window(windowSize) {

    open fun cancel() {
        super.close()
        onCancel()
    }

    open fun submit() {
        super.close()
        onSubmit()
    }
}