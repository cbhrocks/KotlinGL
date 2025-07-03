package org.kotlingl

import org.joml.Vector3f
import org.joml.minus
import org.joml.plus
import org.joml.times
import org.kotlingl.shapes.Ray
import kotlin.math.PI

class Camera(
    var position: Vector3f = Vector3f(0f, 0f, 0f),
    var lookAt: Vector3f = Vector3f(0f,0f,-1f),
    var up: Vector3f = Vector3f(0f,1f,0f),
    var fieldOfView: Float = 90f
) {
    var direction: Vector3f
        get() {
            return (this.lookAt - this.position).normalize()
        }
        set(direction){
            this.lookAt = this.position + direction
        }

    fun generateRays(resX: Int, resY: Int): List<Ray>{
        val right = this.direction.cross(up, Vector3f()).normalize()
        val trueUp = right.cross(this.direction, Vector3f())

        // compute viewport dimensions. Viewport is a plane floating distance 1 in front of the camera
        val fovRadians = this.fieldOfView / 180f * PI.toFloat()
        val viewportHeight = 2f * kotlin.math.tan(fovRadians/2f)
        val aspectRatio = resX.toFloat()/resY.toFloat()
        val viewportWidth = viewportHeight * aspectRatio

        // compute image plane position (origin) and horizontal/vertical vectors
        val focalLength = 1f  // distance from camera to image plane
        val horizontal = right * viewportWidth
        val vertical = trueUp * viewportHeight
        // world space coordinates of lower left corner
        val lowerLeftCorner = this.position + this.direction * focalLength -
                horizontal * 0.5f -
                vertical * 0.5f

        val rays = mutableListOf<Ray>()
        // generate in row major order bottom up
        for (j in 0 until resY) {
            for (i in 0 until resX) {
                // compute x, y coordinates on the image plane (viewport)
                // these are normalized screen-space coordinates
                val u = (i + 0.5f) / resX
                val v = (j + 0.5f) / resY
                // compute world space coordinates of pixel
                val pixelPosition = lowerLeftCorner + horizontal * u + vertical * v

                rays.add(Ray(this.position, (pixelPosition - this.position).normalize()))
            }
        }
        return rays
    }
}

class CameraManager(private val cameras: MutableMap<String, Camera> = mutableMapOf()) {

    fun addCamera(id: String, camera: Camera) {
        cameras[id] = camera
    }

    fun getCamera(name: String): Camera {
        return cameras.getValue(name)
    }
}