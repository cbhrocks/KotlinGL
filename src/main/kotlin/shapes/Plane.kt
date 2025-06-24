package org.kotlingl.shapes

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.minus
import org.joml.plus
import org.joml.times
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import kotlin.math.abs

class Plane(
    var position: Vector3f,
    var normal: Vector3f,
    val material: Material,
    tangent: Vector3f? = null
) : Intersectable {
    var tangent: Vector3f = tangent ?: if (abs(normal.y) < 0.99f)
        Vector3f(0f, 1f, 0f).cross(normal).normalize()
    else
        Vector3f(1f, 0f, 0f).cross(normal).normalize()

    private fun getUVIntersect(hitPoint: Vector3f, origin: Vector3f, tangent: Vector3f, normal: Vector3f): Vector2f {
        val bitangent = normal.cross(tangent, Vector3f()).normalize()
        val local = hitPoint - origin
        val rawU = local.dot(tangent)
        val rawV = local.dot(bitangent)
        // the addition moves the material to be centered on the origin of the plane.
        val scaledU = (rawU / material.uvScale.x()) + 0.5f
        val scaledV = (rawV / material.uvScale.y()) + 0.5f
        return Vector2f(scaledU, scaledV)
    }

    override fun intersects(ray: Ray): Intersection? {
        val denom = ray.direction.dot(this.normal)

        // Ray is parallel to the plane
        if (abs(denom) < 1e-6f) return null

        val t = (this.position - ray.origin).dot(this.normal) / denom
        val hitPoint = ray.origin + ray.direction * t
        val isFrontFace = ray.direction.dot(this.normal) < 0
        val uv = getUVIntersect(hitPoint, position, tangent, normal)

        return if (t >= 0f) Intersection(
            hitPoint,
            this.normal,
            t,
            this.material,
            isFrontFace,
            uv
        ) else null // Only accept intersections in front of the ray
    }
}