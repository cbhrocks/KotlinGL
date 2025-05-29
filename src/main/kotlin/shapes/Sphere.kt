package org.kotlingl.shapes

import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.math.*
import kotlin.math.sqrt

class Sphere(
    val center: Vector3 = Vector3(0f,0f,0f),
    val radius: Float = 1f,
    override val material: Material
): Shape {

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

        return Intersection(
            closestPoint,
            normal,
            closestDistance,
            this.material,
            true,
        )
    }
}