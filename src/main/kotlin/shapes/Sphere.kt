package org.kotlingl.shapes

import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.math.*
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

class Sphere(
    val center: Vector3 = Vector3(0f,0f,0f),
    val radius: Float = 1f,
    override val material: Material,
    val up: Vector3 = Vector3.UNIT_Y,
    val right: Vector3 = Vector3.UNIT_X
): Shape {

    init {
        require(up dot right == 0f) { "Up and Right vectors must equal 0" }
    }

    private fun getUVIntersect(hitPoint: Vector3): Vector2 {
        val local = (hitPoint - center).normalize()

        val forward = up.cross(right).normalize() // local "forward" vector (orthogonal to up & right)

        // Project local point into the new basis
        val x = local.dot(right.normalize())
        val y = local.dot(up.normalize())
        val z = local.dot(forward)

        val u = 0.5f + atan2(z, x) / (2f * Math.PI.toFloat())
        val v = 0.5f - asin(y) / Math.PI.toFloat()

        return Vector2(u, v)
    }

    override fun intersects(ray: Ray): Intersection? {
        val oc = ray.origin - this.center
        val a = ray.direction dot ray.direction
        val b = 2f * (oc dot ray.direction)
        val c = (oc dot oc) - this.radius * this.radius

        val discriminant = b * b - 4f * a * c


        if(discriminant < 0) return null

        // find the intersection distances along the ray
        val sqrtD = sqrt(discriminant)
        val t1 = (-b - sqrtD) / (2f * a)
        val t2 = (-b + sqrtD) / (2f * a)

        val closestDistance = when {
            t1 >= 0 -> t1
            t2 >= 0 -> t2
            else -> return null
        }
        val closestPoint = ray.origin + ray.direction * closestDistance
        val normal = (closestPoint - this.center).normalize()

        val uv = if (material.texture != null) getUVIntersect(closestPoint) else null

        return Intersection(
            closestPoint,
            normal,
            closestDistance,
            this.material,
            true,
            uv
        )
    }
}