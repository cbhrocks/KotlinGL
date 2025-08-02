package org.kotlingl.GameState

import org.joml.Vector2f
import org.kotlingl.Collider.Collider
import org.kotlingl.Input.InputContext
import org.kotlingl.Input.InputEvent
import org.kotlingl.Input.InputManager
import org.kotlingl.Input.KeyAction
import org.kotlingl.model.Model
import org.kotlingl.shapes.Updatable
import org.lwjgl.glfw.GLFW

class PlayerInputContext(private val player: Player) : InputContext {
    override fun handleInput(event: InputEvent) {
        when (event.action) {
            KeyAction.PRESSED -> when (event.key) {
                GLFW.GLFW_KEY_W -> player.setMoveY(1f)
                GLFW.GLFW_KEY_S -> player.setMoveY(-1f)
                GLFW.GLFW_KEY_A -> player.setMoveX(-1f)
                GLFW.GLFW_KEY_D -> player.setMoveX(1f)
                GLFW.GLFW_KEY_SPACE -> player.jump()
                GLFW.GLFW_KEY_J -> player.attack()
                else -> return
            }
            KeyAction.RELEASED -> when (event.key) {
                GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_S -> player.setMoveY(0f)
                GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_D -> player.setMoveX(0f)
                else -> return
            }
        }

        event.consumed = true
    }
}

class Player(
    val model: Model,
    val collider: Collider,
) : Updatable {
    val moveIntent = Vector2f(0f, 0f)

    fun setMoveX(x: Float) { moveIntent.x = x }
    fun setMoveY(y: Float) { moveIntent.y = y }

    var velocity = Vector2f(0f, 0f)
    var isGrounded = false
    var facingLeft = false

    val moveSpeed = 5.0f
    val jumpForce = 10.0f

    override fun update(timeDelta: Float) {
        applyPhysics()
        updateModelTransform()
        updateAnimation()
    }

    fun moveLeft() {
        velocity.x = -moveSpeed
        facingLeft = true
    }

    fun moveRight() {
        velocity.x = moveSpeed
        facingLeft = false
    }

    fun jump() {
        if (isGrounded) {
            velocity.y = jumpForce
            isGrounded = false
        }
    }

    fun attack() {
        // logic to trigger attack animation, damage check, etc
    }

    private fun applyPhysics() {
        velocity.y -= 0.5f // gravity
        // collider.move(velocity)

        // isGrounded = collider.checkGroundContact()
    }

    private fun updateModelTransform() {
        // model.position.set(collider.position.x, collider.position.y, 0f)
        model.scale.set(if (facingLeft) -1f else 1f, 1f, 1f) // flip for direction
    }

    private fun updateAnimation() {
        // if (!isGrounded) {
        //     animator.play("jump")
        // } else if (velocity.x != 0f) {
        //     animator.play("run")
        // } else {
        //     animator.play("idle")
        // }
    }
}