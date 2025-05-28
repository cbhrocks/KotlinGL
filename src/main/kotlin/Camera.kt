package org.kotlingl

import org.kotlingl.math.*
import org.kotlingl.shapes.Ray
import org.lwjgl.opengl.GL11
import java.nio.ByteBuffer
import kotlin.math.PI

class Camera(
    var position: Vector3 = Vector3(0f,0f,0f),
    var lookAt: Vector3 = Vector3(0f,0f,1f),
    var up: Vector3 = Vector3(0f,1f,0f),
    var resX: Int = 480,
    var resY: Int = 240,
    var fieldOfView: Float = 120f
) {
    var textureId: Int? = null

    init {
    }

    fun initGL() {
        // create opengl texture
        this.textureId = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId!!)
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGB,
            this.resX,
            this.resY,
            0,
            GL11.GL_RGB,
            GL11.GL_UNSIGNED_BYTE,
            null as ByteBuffer?
        )
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    }

    var FoVRadians: Float
        get() {
            return this.fieldOfView / 180f * PI.toFloat()
        }
        set(radians){
            this.fieldOfView = radians * 180 / PI.toFloat()
        }

    var direction: Vector3
        get() {
            return (this.lookAt - this.position).normalize()
        }
        set(direction){
            this.lookAt = this.position + direction
        }

    val aspectRatio: Float
        get() {
            return this.resX/this.resY.toFloat()
        }

    private fun generateRay(
        pixelX: Int,
        pixelY: Int
    ): Ray {
        val right = (this.direction cross up).normalize()
        val trueUp = (right cross this.direction)

        // compute viewport dimensions. Viewport is a plane floating distance 1 in front of the camera
        val viewportHeight = 2 * kotlin.math.tan(this.FoVRadians/2)
        val viewportWidth = viewportHeight * aspectRatio

        // compute image plane position (origin) and horizontal/vertical vectors
        val focalLength = 1f  // distance from camera to image plane
        val horizontal = right * viewportWidth
        val vertical = trueUp * viewportHeight
        // world space coordinates of lower left corner
        val lowerLeftCorner = this.position + this.direction * focalLength -
                horizontal * 0.5f -
                vertical * 0.5f

        // compute x, y coordinates on the image plane (viewport)
        // these are normalized screen-space coordinates
        val u = (pixelX + 0.5f) / this.resX
        val v = (pixelY + 0.5f) / this.resY
        // compute world space coordinates of pixel
        val pixelPosition = lowerLeftCorner + horizontal * u + vertical * v

        return Ray(this.position, (pixelPosition - this.position).normalize())
    }

    fun generateRays(): List<Ray>{
        val rays = mutableListOf<Ray>()
        // generate in row major order bottom up
        for (j in 0 until this.resY) {
            for (i in 0 until this.resX) {
                rays.add(generateRay(i, j))
            }
        }
        return rays
    }
}