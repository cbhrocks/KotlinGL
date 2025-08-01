package org.kotlingl.GameState

import org.joml.Vector2f
import org.kotlingl.Collider.Collider
import org.kotlingl.model.Model
import org.kotlingl.shapes.Updatable

class Player(
    val model: Model,
    val collider: Collider,
) : Updatable {

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
        collider.move(velocity)

        isGrounded = collider.checkGroundContact()
    }

    private fun updateModelTransform() {
        model.position.set(collider.position.x, collider.position.y, 0f)
        model.scale.set(if (facingLeft) -1f else 1f, 1f, 1f) // flip for direction
    }

    private fun updateAnimation() {
        if (!isGrounded) {
            animator.play("jump")
        } else if (velocity.x != 0f) {
            animator.play("run")
        } else {
            animator.play("idle")
        }
    }
}