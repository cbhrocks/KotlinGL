package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Shape
import org.kotlingl.shapes.Triangle
import org.lwjgl.assimp.AIColor4D
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.Assimp
import java.io.File
import javax.swing.text.Position

import org.joml.Vector3f

class Model(
    val meshes: List<Mesh>,
    val materials: List<Material>,
    val bones: List<Bone> = listOf(),
    var position: Vector3f = Vector3f(0f,0f,0f),
    var rotation: Quaternionf = Quaternionf(),
    var scale: Vector3f = Vector3f(0f, 0f, 0f)
) : Shape {
    var meshTriangles = HashMap<Int, List<Triangle>>()

    val transform: Matrix4f
        get() = Matrix4f.translation(position) *
                Matrix4f.rotation(rotation) *
                Matrix4f.scale(scale)

    override fun intersects(ray: Ray): Intersection? {
        var closestHit: Intersection? = null

        val localRay = ray.transformedBy(transform.inverse())

        val localHit = meshes.mapIndexedNotNull { i, it ->
            this.meshTriangles.getOrPut(i) { it.getTriangles(materials[it.materialIndex]) }
                .mapNotNull { it.intersects(localRay) }
                .minByOrNull { it.t }
        }.minByOrNull { it.t } ?: return null

        return localHit.transformedBy(transform)
    }

    /* for when rasterization is implemented
    fun Model.draw(shader: Shader) {
        shader.setMatrix("model", transform)
        for (mesh in meshes) {
            mesh.draw(shader)
        }
        children.forEach { it.draw(shader) }
    }
    */

    companion object {
        fun fromAssimp(path: String): Model {
            val flags = Assimp.aiProcess_Triangulate or Assimp.aiProcess_GenSmoothNormals or Assimp.aiProcess_FlipUVs

            val url = object {}.javaClass.getResource(path)
            val path = File(url!!.toURI()).absolutePath

            val scene = Assimp.aiImportFile(path, flags)
                ?: throw RuntimeException("Error loading model: ${Assimp.aiGetErrorString()}")

            val materials = mutableListOf<Material>()
            val meshes = mutableListOf<Mesh>()

            // Load materials
            for (i in 0 until scene.mNumMaterials()) {
                val aiMaterial = AIMaterial.create(scene.mMaterials()!![i])
                val color = AIColor4D.create()
                Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_NONE, 0, color)
                materials += Material(
                    ColorRGB(
                        (color.r() * 255).toInt(),
                        (color.g() * 255).toInt(),
                        (color.b() * 255).toInt()
                    )
                )
            }

            // Load meshes
            for (i in 0 until scene.mNumMeshes()) {
                val aiMesh = AIMesh.create(scene.mMeshes()!![i])

                val vertices = mutableListOf<Vertex>()
                val indices = mutableListOf<Int>()

                for (j in 0 until aiMesh.mNumVertices()) {
                    val aiPos = aiMesh.mVertices()[j]
                    val position = Vector3(aiPos.x(), aiPos.y(), aiPos.z())

                    val aiNorm = aiMesh.mNormals()?.get(j)
                    if (aiNorm == null) {
                        throw RuntimeException("no normal found")
                    }
                    val normal = Vector3(aiNorm.x(), aiNorm.y(), aiNorm.z())

                    val texCoords = aiMesh.mTextureCoords(0)
                    val uv: Vector2
                    if (texCoords != null) {
                        val aiUV = texCoords[j]
                        uv = Vector2(aiUV.x(), aiUV.y())
                    } else {
                        uv = Vector2(0f, 0f)
                    }
                    vertices += Vertex(
                        position,
                        normal,
                        uv
                    )
                }

                for (j in 0 until aiMesh.mNumFaces()) {
                    val face = aiMesh.mFaces()[j]
                    for (k in 0 until face.mNumIndices()) {
                        indices += face.mIndices()[k]
                    }
                }

                meshes += Mesh(vertices, indices, aiMesh.mMaterialIndex())
            }

            Assimp.aiReleaseImport(scene)

            return Model(meshes, materials)
        }
    }
}

