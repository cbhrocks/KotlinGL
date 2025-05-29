package org.kotlingl.shapes

import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.math.*
import kotlin.math.abs

class Plane(
    var position: Vector3,
    var normal: Vector3,
    override val material: Material
): Shape {
    override fun intersects(ray: Ray): Intersection? {
        val denom = ray.direction dot this.normal

        // Ray is parallel to the plane
        if (abs(denom) < 1e-6f) return null

        val t = (this.position - ray.origin).dot(this.normal) / denom
        val hitPoint = ray.origin + ray.direction * t
        val isFrontFace = ray.direction.dot(this.normal) < 0

        return if (t >= 0f) Intersection(
            hitPoint,
            this.normal,
            t,
            this.material,
            isFrontFace
        ) else null // Only accept intersections in front of the ray
    }
}