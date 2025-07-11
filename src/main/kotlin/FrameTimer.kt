package org.kotlingl

import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class FrameTimer {
    private var lastTime = System.nanoTime()
    var deltaTime = 0.0f
        private set
    var totalTime = 0.0f
        private set
    val updateHistory = ArrayDeque<Long>()
    var fps = 0
        private set

    fun update() {
        val currentTime = System.nanoTime()
        deltaTime = (currentTime - lastTime) / 1_000_000_000.0f
        lastTime = currentTime
        totalTime += deltaTime

        updateHistory.addFirst(lastTime)
        var oldTime = updateHistory.last()
        while ((currentTime - oldTime).nanoseconds > 1.seconds) {
            updateHistory.removeLast()
            oldTime = updateHistory.last()
        }
        fps = updateHistory.size
    }
}