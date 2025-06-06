package org.kotlingl.shapes

import org.joml.Vector3f
import org.joml.minus
import org.joml.plus
import org.joml.times
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.math.*
import org.kotlingl.model.Vertex

// use winding order same as opengl. counter-clockwise for front face, clockwise for backface
class Triangle (
    val v0: Vertex,
    val v1: Vertex,
    val v2: Vertex,
    val material: Material,
) : Shape {

    private var faceNormal: Vector3f = (v1.position - v0.position).cross(v2.position - v0.position).normalize()

    // Möller–Trumbore Triangle-Ray Intersection
    override fun intersects(ray: Ray): Intersection? {
        val edge1 = v1.position - v0.position
        val edge2 = v2.position - v0.position
        val h = ray.direction.cross(edge2, Vector3f())
        val a = edge1.dot(h)

        // Ray is parallel to triangle
        if (a > -EPSILON && a < EPSILON) {
            return null
        }

        val f = 1f / a
        val s = ray.origin - v0.position
        val u = f * s.dot(h)

        if (u < 0.0f || u > 1.0f) {
            return null
        }

        val q = s.cross(edge1)
        val v = f * ray.direction.dot(q)

        if (v < 0.0f || u + v > 1.0f) {
            return null
        }

        // At this stage, we can compute t to find out where the intersection point is on the line
        val t = f * edge2.dot(q)

        val uv = if (material.texture != null) {
            val w = 1f - u - v
            v0.uv * w + v1.uv * u + v2.uv * v
        } else null

        return if (t > EPSILON) {
            Intersection(
                ray.origin + ray.direction * t,
                faceNormal,
                t,
                material,
                faceNormal.dot(ray.origin + ray.direction) > 0f,
                uv
            )
        } else null // Ray intersection, return distance t
    }
}