package org.kotlingl.shapes

import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.math.*

// use winding order same as opengl. counter-clockwise for front face, clockwise for backface
class Triangle (
    val v0: Vector3,
    val v1: Vector3,
    val v2: Vector3,
    override val material: Material,
    normal: Vector3? = null,
    uv0: Vector2? = null,
    uv1: Vector2? = null,
    uv2: Vector2? = null,
) : Shape {
    val normal: Vector3
    val uv0: Vector2
    val uv1: Vector2
    val uv2: Vector2

    init {
        val edge1 = v1 - v0
        val edge2 = v2 - v0
        this.normal = normal ?: edge1.cross(edge2).normalize()
        this.uv0 = uv0 ?: Vector2(0f, 0f)
        this.uv1 = uv1 ?: Vector2(1f, 0f)
        this.uv2 = uv2 ?: Vector2(1f, 1f)
    }

    // Möller–Trumbore Triangle-Ray Intersection
    override fun intersects(ray: Ray): Intersection? {
        val edge1 = v1 - v0
        val edge2 = v2 - v0
        val h = ray.direction cross edge2
        val a = edge1 dot h

        // Ray is parallel to triangle
        if (a > -EPSILON && a < EPSILON) {
            return null
        }

        val f = 1f / a
        val s = ray.origin - v0
        val u = f * s.dot(h)

        if (u < 0.0f || u > 1.0f) {
            return null
        }

        val q = s cross edge1
        val v = f * ray.direction.dot(q)

        if (v < 0.0f || u + v > 1.0f) {
            return null
        }

        // At this stage, we can compute t to find out where the intersection point is on the line
        val t = f * edge2.dot(q)

        val uv = if (material.texture != null) {
            val w = 1f - u - v
            uv0 * w + uv1 * u + uv2 * v
        } else null

        return if (t > EPSILON) {
            Intersection(
                ray.origin + ray.direction * t,
                normal,
                t,
                material,
                normal dot (ray.origin + ray.direction) > 0f,
                uv
            )
        } else null // Ray intersection, return distance t
    }
}