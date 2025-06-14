package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.times
import org.kotlingl.entity.Material
import org.kotlingl.entity.Texture
import org.kotlingl.entity.toColor
import org.kotlingl.math.toJoml
import org.lwjgl.assimp.AIBone
import org.lwjgl.assimp.AIColor4D
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.AIString
import org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_DIFFUSE
import org.lwjgl.assimp.Assimp.aiGetErrorString
import org.lwjgl.assimp.Assimp.aiGetMaterialColor
import org.lwjgl.assimp.Assimp.aiGetMaterialTexture
import org.lwjgl.assimp.Assimp.aiImportFile
import org.lwjgl.assimp.Assimp.aiProcess_FlipUVs
import org.lwjgl.assimp.Assimp.aiProcess_GenSmoothNormals
import org.lwjgl.assimp.Assimp.aiProcess_Triangulate
import org.lwjgl.assimp.Assimp.aiReleaseImport
import org.lwjgl.assimp.Assimp.aiTextureType_DIFFUSE
import org.lwjgl.assimp.Assimp.aiTextureType_NONE
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.collections.joinToString
import kotlin.collections.plusAssign
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.toPath


data class ModelCacheData (
    val name: String,
    val children: List<ModelCacheData>,
    val meshes: List<Mesh>,
)

class ModelLoader {
    // the user defined name pointing to the file path used to import the model
    val modelLookup: MutableMap<String, String> = mutableMapOf()
    // the file name pointing to the ModelCacheData used for reconstruction.
    val modelCache: MutableMap<String, ModelCacheData> = mutableMapOf()
    val skeletonCache: MutableMap<Int, BoneNode> = mutableMapOf()

    private fun normalizePath(path: String): String =
        Paths.get(path).normalize().toString().replace('\\', '/')

    fun createModel(lookupName: String): Model {
        val filePath = modelLookup[lookupName] ?: error("Model $lookupName hasn't been loaded.")
        return buildModel(modelCache.getValue(filePath))
    }

    private fun buildModel(modelData: ModelCacheData): Model {
        return Model(
            modelData.name,
            modelData.meshes,
            modelData.skeleton,
            modelData.children.map {buildModel(it)}.toMutableList()
        )
    }

    fun loadModel(filePath: String, name: String) {
        if (modelCache.containsKey(filePath)) throw IllegalArgumentException("model already loaded!")
        if (modelLookup.containsKey(name)) throw IllegalArgumentException("model already exists with that name!")
        importModel(filePath)
    }

    private fun importModel(resourcePath: String) {
        //val path = normalizePath(filepath)

        val flags = aiProcess_Triangulate or aiProcess_GenSmoothNormals or aiProcess_FlipUVs

        val url = object {}.javaClass.getResource(resourcePath)
        val path = url!!.toURI().toPath()

        val scene = aiImportFile(path.absolute().toString(), flags)
            ?: throw RuntimeException("Error loading scene: ${aiGetErrorString()}")

        val directory = Path(resourcePath).parent

        val rootNode = scene.mRootNode() ?: throw RuntimeException( "failed to load model from scene." )

        val materials = List(scene.mNumMaterials()) { i ->
            importMaterial(AIMaterial.create(scene.mMaterials()!![i]), directory)
        }
        val aiMeshes = List(scene.mNumMeshes()) { i ->
            AIMesh.create(scene.mMaterials()!![i])
        }
        aiMeshes.map {
            importMesh(it, materials[it.mMaterialIndex()])
        }

        val boneNames = aiMeshes.map { aiMesh ->
            List(aiMesh.mNumBones()) { i ->
                AIBone.create(aiMesh.mBones()!![i])
            }.map {
                it.mName().toString()
            }
        }.flatten().toSet()

        val rootBoneNode = importSkeleton(boneNames)
        val skeleton = skeletonCache.getOrDefault(rootBoneNode.hashCode(), rootBoneNode)

        val modelCacheData = importNode(directory, scene, rootNode, boneNames)

        aiReleaseImport(scene)
    }

    fun importMaterial(material: AIMaterial, modelDirectory: Path): Material {
        val color = AIColor4D.create()
        aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color)

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
            val texturePath = pathBuffer.dataString().replace("\\", "/")
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

    fun importMesh(aiMesh: AIMesh, material: Material): Mesh {
        val vertices = List(aiMesh.mNumVertices()) { i ->
            val aiPos = aiMesh.mVertices()[i]
            val position = Vector3f(aiPos.x(), aiPos.y(), aiPos.z())

            val aiNorm = aiMesh.mNormals()?.get(i)
            if (aiNorm == null) {
                throw RuntimeException("no normal found")
            }
            val normal = Vector3f(aiNorm.x(), aiNorm.y(), aiNorm.z())

            val texCoords = aiMesh.mTextureCoords(0)
            val uv: Vector2f
            if (texCoords != null) {
                val aiUV = texCoords[i]
                uv = Vector2f(aiUV.x(), aiUV.y())
            } else {
                uv = Vector2f(0f, 0f)
            }
            Vertex( position, normal, uv )
        }

        val indices = mutableListOf<Int>()
        for (j in 0 until aiMesh.mNumFaces()) {
            val face = aiMesh.mFaces()[j]
            for (k in 0 until face.mNumIndices()) {
                indices += face.mIndices()[k]
            }
        }

        val meshBones = List(aiMesh.mNumBones()) { i ->
            val aiBone = AIBone.create(aiMesh.mBones()!![i])
            val boneName = aiBone.mName().dataString()
            val offsetMatrix = aiBone.mOffsetMatrix().toJoml()

            val weights = List(aiBone.mNumWeights()) { j ->
                val weight = aiBone.mWeights()!![j]
                VertexWeight(weight.mVertexId(), weight.mWeight())
            }

            Bone(boneName, offsetMatrix, weights)
        }

        return Mesh(vertices, indices, material, meshBones)
    }

    fun importSkeleton(
        boneNames: Set<String>,
        node: AINode,
        parentGlobalTransform: Matrix4f = Matrix4f(),
    ): BoneNode {
        val name = node.mName().dataString()
        // where each bone/model is relative to it's parent
        val localTransform = node.mTransformation().toJoml()
        // where each bone is in world space
        val globalTransform = parentGlobalTransform.mul(localTransform, Matrix4f())

        var children = List(node.mNumChildren()) { i ->
            val childNode = AINode.create(node.mChildren()!![i])
            importSkeleton(boneNames, childNode, globalTransform)
        }

        val boneNode = BoneNode(
            name,
            localTransform,
            globalTransform,
            children,
        )
        for (child in boneNode.children) {
            child.parent = boneNode
        }
        return boneNode
    }

    fun importNode(
        directory: Path,
        scene: AIScene,
        node: AINode,
        boneNames: Set<String>,
        parentModelTransform: Matrix4f = Matrix4f(),
    ): ModelCacheData {
        val name = node.mName().dataString()
        // where each bone/model is relative to it's parent
        val localTransform = node.mTransformation().toJoml()
        // where each bone is in world space
        val globalTransform = parentGlobalTransform.mul(localTransform, Matrix4f())
        // where each model is in world space
        val modelTransform = parentModelTransform.mul(localTransform, Matrix4f())

        val childrenData = List(node.mNumChildren()) { i ->
            val childNode = AINode.create(node.mChildren()!![i])
            importNode(directory, scene, childNode, boneNames, modelTransform, globalTransform)
        }

        // add bone node
        val boneNode = BoneNode(
            name,
            localTransform,
            globalTransform,
        )

        // Create the model for this node
        val model = Model(
            meshes = meshes,
            name = name
        ).apply {
            modelTransform
        }

        // Recursively load children
        val children: MutableList<String> = mutableListOf()
        for (i in 0 until node.mNumChildren()) {
        }
    }
}