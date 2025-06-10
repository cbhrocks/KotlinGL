package org.kotlingl.shapes

import org.joml.Vector3f
import org.kotlingl.entity.Intersection
import org.kotlingl.model.BVHNode

interface Intersectable {
    //val aabb: AABB
    fun intersects(ray: Ray): Intersection?
}

interface Bounded : Intersectable {
    fun getBVHNode(): BVHNode
    fun computeAABB(): AABB
    fun centroid(): Vector3f
}