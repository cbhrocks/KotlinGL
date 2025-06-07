package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.kotlingl.entity.Intersection
import org.kotlingl.entity.Material
import org.kotlingl.shapes.Ray
import org.kotlingl.shapes.Shape
import org.kotlingl.shapes.Triangle
import org.lwjgl.assimp.AIColor4D
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMesh
import java.io.File

import org.joml.Vector3f
import org.kotlingl.entity.Texture
import org.kotlingl.entity.toColor
import org.kotlingl.math.toJoml
import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.AIString
import org.lwjgl.assimp.Assimp.*
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.toPath

class Model(
    val meshes: List<Mesh>,
    val bones: List<Bone> = listOf(),
    private var position: Vector3f = Vector3f(0f,0f,0f),
    private var rotation: Quaternionf = Quaternionf(),
    private var scale: Vector3f = Vector3f(1f, 1f, 1f)
) : Shape {
    var meshTriangles = HashMap<Int, List<Triangle>>()

    private var modelM: Matrix4f = Matrix4f()
    private var modelMInverse: Matrix4f = Matrix4f()
    private var children: MutableList<Model> = mutableListOf()

    fun transform(
        mat: Matrix4f
    ) {
        this.modelM = mat
        mat.invert(this.modelMInverse)
    }

    fun transform(
        position: Vector3f = this.position,
        rotation: Quaternionf = this.rotation,
        scale: Vector3f = this.scale
    ) {
        this.modelM = this.modelM.identity().translation(position)
            .rotation(rotation)
            .scale(scale)
        this.modelM.invert(this.modelMInverse)
    }

    init {
        transform()
    }

    fun addChild(model: Model) {
        this.children.add(model)
    }

    override fun intersects(ray: Ray): Intersection? {
        val localRay = ray.transformedBy(modelMInverse)
        //val localRay = ray//.transformedBy(modelMInverse)

        val localHit = (meshes.mapIndexedNotNull { i, it ->
            this.meshTriangles.getOrPut(i) { it.getTriangles() }
                .mapNotNull { it2 -> it2.intersects(localRay) }
                .minByOrNull { it2 -> it2.t }
        } + children.mapNotNull { it.intersects(localRay) })
            .minByOrNull { it.t }

        //return localHit

        return localHit?.let {
            val hitPointWorld = modelM.transformPosition(localHit.point, Vector3f())
            val hitWorldNormal = modelM.transformDirection(localHit.normal, Vector3f()).normalize()
            val frontFace = ray.direction.dot(hitWorldNormal) < 0

            Intersection(
                hitPointWorld,
                hitWorldNormal,
                localHit.t,
                localHit.material,
                frontFace,
                localHit.uv
            )
        }
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
        fun loadMaterial(material: AIMaterial, modelDirectory: Path): Material {
            val color = AIColor4D.create()
            aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color)

            val r = color.r()
            val g = color.g()
            val b = color.b()

            // Check for diffuse texture
            val pathBuffer = AIString.calloc()
            var texture: Texture? = null

            // Explicitly typed nulls
            val mapping: IntBuffer? = null
            val uvindex: IntBuffer? = null
            val blend: FloatBuffer? = null
            val op: IntBuffer? = null
            val mapmode: IntBuffer? = null
            val flags: IntBuffer? = null

            if (aiGetMaterialTexture(
                    material,
                    aiTextureType_DIFFUSE,
                    0,
                    pathBuffer,
                    mapping,
                    uvindex,
                    blend,
                    op,
                    mapmode,
                    flags
                ) == 0) {
                val texturePath = pathBuffer.dataString()
                val fullPath = "/" + modelDirectory.resolve(texturePath).normalize().joinToString("/")

                texture = Texture.fromImageResource(fullPath) // Your own Texture loader
            }

            // Add more properties like reflectivity, shininess, etc., if needed
            return Material(
                Vector3f(
                    color.r(),
                    color.g(),
                    color.b()
                ).toColor(),
                texture,
            )
        }

        fun loadMesh(aiMesh: AIMesh, transform: Matrix4f, material: Material): Mesh {
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

            return Mesh(vertices, indices, material)
        }

        fun loadModel(
            directory: Path,
            scene: AIScene,
            node: AINode,
            transform: Matrix4f = Matrix4f(),
            materials: List<Material>? = null
        ): Model {
            val nodeTransform = node.mTransformation().toJoml().mul(transform)

            // load materials
            val loadedMaterials = materials ?: List(scene.mNumMaterials()) { i ->
                loadMaterial(
                    AIMaterial.create(scene.mMaterials()!![i]),
                    directory
                )
            }

            // Collect meshes for this node
            val meshes = mutableListOf<Mesh>()
            for (i in 0 until node.mNumMeshes()) {
                val meshIndex = node.mMeshes()!![i]
                val aiMesh = AIMesh.create(scene.mMeshes()!![meshIndex])

                val materialIndex = aiMesh.mMaterialIndex()
                val material = loadedMaterials[materialIndex]

                meshes.add(loadMesh(aiMesh, transform, material))
            }

            // Create the model for this node
            val model = Model(
                meshes = meshes
            ).apply { transform(nodeTransform) }

            // Recursively load children
            for (i in 0 until node.mNumChildren()) {
                val childNode = AINode.create(node.mChildren()!![i])
                val childModel = loadModel(directory, scene, childNode, nodeTransform)
                model.addChild(childModel)
            }

            return model
        }

        fun fromAssimp(resourcePath: String): Model {
            val flags = aiProcess_Triangulate or aiProcess_GenSmoothNormals or aiProcess_FlipUVs

            val url = object {}.javaClass.getResource(resourcePath)
            val path = url!!.toURI().toPath()

            val scene = aiImportFile(path.absolute().toString(), flags)
                ?: throw RuntimeException("Error loading scene: ${aiGetErrorString()}")

            val directory = Path(resourcePath).parent

            val rootNode = scene.mRootNode() ?: throw RuntimeException( "failed to load model from scene." )
            val model = loadModel(directory, scene, rootNode)

            aiReleaseImport(scene)
            return model
        }
    }
}

