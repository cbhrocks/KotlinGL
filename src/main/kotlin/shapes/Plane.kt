package org.kotlingl.shapes

import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.math.*
import kotlin.math.abs

class Plane(
    var position: Vector3,
    var normal: Vector3,
    override val material: Material,
    tangent: Vector3? = null
) : Shape {
    var tangent: Vector3

    init {
        this.tangent = tangent ?: if (abs(normal.y) < 0.99f)
            Vector3(0f, 1f, 0f).cross(normal).normalize()
        else
            Vector3(1f, 0f, 0f).cross(normal).normalize()
    }

    private fun getUVIntersect(hitPoint: Vector3, origin: Vector3, tangent: Vector3, normal: Vector3): Vector2 {
        val bitangent = normal.cross(tangent).normalize()
        val local = hitPoint - origin
        val rawU = local.dot(tangent)
        val rawV = local.dot(bitangent)
        // the addition moves the material to be centered on the origin of the plane.
        val scaledU = (rawU / material.uvScale.x) + 0.5f
        val scaledV = (rawV / material.uvScale.y) + 0.5f
        val wrappedUV = material.getWrappedUV(Vector2(scaledU, scaledV))
        return wrappedUV
    }

    override fun intersects(ray: Ray): Intersection? {
        val denom = ray.direction dot this.normal

        // Ray is parallel to the plane
        if (abs(denom) < 1e-6f) return null

        val t = (this.position - ray.origin).dot(this.normal) / denom
        val hitPoint = ray.origin + ray.direction * t
        val isFrontFace = ray.direction.dot(this.normal) < 0
        val uv = if (material.texture != null) getUVIntersect(hitPoint, position, tangent, normal) else null

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