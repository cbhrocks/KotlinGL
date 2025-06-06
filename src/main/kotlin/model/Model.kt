package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.kotlingl.entity.ColorRGB
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Shape
import org.kotlingl.shapes.Triangle
import org.lwjgl.assimp.AIColor4D
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.Assimp
import java.io.File

import org.joml.Vector3f

class Model(
    val meshes: List<Mesh>,
    val materials: List<Material>,
    val bones: List<Bone> = listOf(),
    var initialPosition: Vector3f = Vector3f(0f,0f,0f),
    var initialRotation: Quaternionf = Quaternionf(),
    var initialScale: Vector3f = Vector3f(0f, 0f, 0f)
) : Shape {
    var meshTriangles = HashMap<Int, List<Triangle>>()

    private lateinit var transform: Matrix4f
    private lateinit var transformInverse: Matrix4f

    var position: Vector3f = initialPosition
        set(value) {
            field = value
            createTransforms()
        }
    var rotation: Quaternionf = initialRotation
        set(value) {
            field = value
            createTransforms()
        }
    var scale: Vector3f = initialScale
        set(value) {
            field = value
            createTransforms()
        }

    private fun createTransforms() {
        this.transform = Matrix4f().translation(position)
            .rotation(rotation)
            .scale(scale)
        this.transformInverse = Matrix4f(this.transform).invert()
    }

    init {
        createTransforms()
    }


    override fun intersects(ray: Ray): Intersection? {
        var closestHit: Intersection? = null

        val localRay = ray.transformedBy(transformInverse)

        val localHit = meshes.mapIndexedNotNull { i, it ->
            this.meshTriangles.getOrPut(i) { it.getTriangles(materials[it.materialIndex]) }
                .mapNotNull { it.intersects(localRay) }
                .minByOrNull { it.t }
        }.minByOrNull { it.t } ?: return null

        return if (localHit != null) {
            val hitPointWorld = transform.transformPosition(localHit.point, Vector3f())
            Intersection(
                hitPointWorld,
                transform.transformDirection(localHit.normal, Vector3f()).normalize(),
                localHit.t,
                localHit.material,
                localHit.frontFace,
                localHit.uv
            )
        } else null
        //return localHit.apply { point.mulPosition(transform) }

        //return localHit.transformedBy(transform)
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
                    val position = Vector3f(aiPos.x(), aiPos.y(), aiPos.z())

                    val aiNorm = aiMesh.mNormals()?.get(j)
                    if (aiNorm == null) {
                        throw RuntimeException("no normal found")
                    }
                    val normal = Vector3f(aiNorm.x(), aiNorm.y(), aiNorm.z())

                    val texCoords = aiMesh.mTextureCoords(0)
                    val uv: Vector2f
                    if (texCoords != null) {
                        val aiUV = texCoords[j]
                        uv = Vector2f(aiUV.x(), aiUV.y())
                    } else {
                        uv = Vector2f(0f, 0f)
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

