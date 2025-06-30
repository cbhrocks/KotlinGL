package org.kotlingl

class FrameTimer {
    private var lastTime = System.nanoTime()
    var deltaTime = 0.0f
        private set
    var totalTime = 0.0f

    fun update() {
        val currentTime = System.nanoTime()
        deltaTime = (currentTime - lastTime) / 1_000_000_000.0f
        lastTime = currentTime
        totalTime += deltaTime
    }
}