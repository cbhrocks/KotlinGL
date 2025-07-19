package org.kotlingl.shapes

import ShaderProgram
import org.joml.Vector3f
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material

abstract class GLResource {
    var glInitialized: Boolean = false
        private set

    abstract fun initGL()
    open fun cleanupGL() {}

    protected fun markInitialized() {
        glInitialized = true
    }

}

interface Drawable {
    fun draw(shader: ShaderProgram)
}

interface Intersectable {
    //val aabb: AABB
    fun intersects(ray: Ray, material: Material? = null): Intersection?
}

interface Updatable {
    fun update(timeDelta: Float)
}

interface Bounded : Intersectable {
    //fun getBVHNode(): BVHNode
    fun computeAABB(): AABB
    fun centroid(): Vector3f
}