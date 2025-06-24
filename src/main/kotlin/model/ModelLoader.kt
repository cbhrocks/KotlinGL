package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.kotlingl.entity.Material
import org.kotlingl.entity.Texture
import org.kotlingl.entity.WrapMode
import org.kotlingl.entity.toColor
import org.kotlingl.math.toJoml
import org.lwjgl.BufferUtils
import org.lwjgl.assimp.AIBone
import org.lwjgl.assimp.AIColor4D
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.AIString
import org.lwjgl.assimp.AITexture
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.stb.STBImage.stbi_failure_reason
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryStack
import java.lang.IllegalStateException
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
    var skeletonHash: Int? = null
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
        val model = buildModel(modelCache.getValue(filePath))
        return model
    }

    private fun buildModel(modelData: ModelCacheData): Model {
        return Model(
            modelData.name,
            modelData.meshes,
            skeletonCache[modelData.skeletonHash],
            modelData.children.map {buildModel(it)}.toMutableList()
        )
    }

    fun loadModel(filePath: String, name: String) {
        if (modelCache.containsKey(filePath)) throw IllegalArgumentException("model already loaded!")
        if (modelLookup.containsKey(name)) throw IllegalArgumentException("model already exists with that name!")
        importModel(filePath)
        modelLookup[name] = filePath
    }

    private fun importModel(resourcePath: String) {
        //val path = normalizePath(filepath)

        val flags = aiProcess_GlobalScale or aiProcess_Triangulate or aiProcess_GenSmoothNormals or aiProcess_FlipUVs

        val url = object {}.javaClass.getResource(resourcePath)
        val path = url!!.toURI().toPath()

        val scene = aiImportFile(path.absolute().toString(), flags)
            ?: throw RuntimeException("Error loading scene: ${aiGetErrorString()}")

        val directory = Path(resourcePath).parent

        val rootNode = scene.mRootNode() ?: throw RuntimeException( "failed to load model from scene." )

        val textures = List(scene.mNumTextures()) { i ->
            importTexture(AITexture.create(scene.mTextures()!![i]))
        }
        val materials = List(scene.mNumMaterials()) { i ->
            importMaterial(AIMaterial.create(scene.mMaterials()!![i]), directory)
        }
        val aiMeshes = List(scene.mNumMeshes()) { i ->
            AIMesh.create(scene.mMeshes()!![i])
        }
        val meshes = aiMeshes.map {
            importMesh(it, materials[it.mMaterialIndex()])
        }

        val boneNames = aiMeshes.map { aiMesh ->
            List(aiMesh.mNumBones()) { i ->
                AIBone.create(aiMesh.mBones()!![i])
            }.map {
                it.mName().toString()
            }
        }.flatten().toSet()

        val rootBoneNode = importSkeleton(boneNames, rootNode)
        val skeletonHash = rootBoneNode.hashCode()
        skeletonCache[skeletonHash] = rootBoneNode

        val modelCacheData = importNode(rootNode, meshes)
        modelCacheData.skeletonHash = skeletonHash
        modelCache[resourcePath] = modelCacheData

        aiReleaseImport(scene)
    }

    fun aiToWrapMode(mode: Int): WrapMode = when (mode) {
        aiTextureMapMode_Wrap -> WrapMode.REPEAT
        aiTextureMapMode_Clamp -> WrapMode.CLAMP
        aiTextureMapMode_Mirror -> WrapMode.MIRROR
        else -> WrapMode.REPEAT
    }

    fun importTexture(texture: AITexture): Texture {
        val isCompressed = texture.mHeight() == 0
        if (!isCompressed) {
            throw UnsupportedOperationException("Only compressed embedded textures are supported.")
        }

        // the width is the size in bytes when compressed
        val dataSize = texture.mWidth()
        val dataBuffer = texture.pcDataCompressed() ?: throw IllegalStateException("No texture data!")

        // Decode using STBImage
        MemoryStack.stackPush().use { stack ->
            val xBuf = stack.mallocInt(1)
            val yBuf = stack.mallocInt(1)
            val compBuf = stack.mallocInt(1)

            // STBImage will decode from memory
            val decoded = stbi_load_from_memory(dataBuffer, xBuf, yBuf, compBuf, 4)
                ?: throw RuntimeException("Failed to decode embedded texture: ${stbi_failure_reason()}")

            val width = xBuf[0]
            val height = yBuf[0]

            // The 'decoded' ByteBuffer is now ready to use (RGBA format)
            return Texture(decoded, width, height, wrapU, wrapV)
        }
    }

    fun importMaterial(material: AIMaterial, modelDirectory: Path): Material {
        val color = AIColor4D.create()
        aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color)

        // Check for diffuse texture

        val pathBuffer = AIString.calloc()
        var diffuseTexture: Texture? = null
        val mapping = BufferUtils.createIntBuffer(1)
        val uvIndex = BufferUtils.createIntBuffer(1)
        val blend = BufferUtils.createFloatBuffer(1)
        val op = BufferUtils.createIntBuffer(1)
        val mapMode = BufferUtils.createIntBuffer(2)
        val flags = BufferUtils.createIntBuffer(1)

        if (aiGetMaterialTexture(
                material,
                aiTextureType_DIFFUSE,
                0,
                pathBuffer,
                mapping,
                uvIndex,
                blend,
                op,
                mapMode,
                flags
            ) == aiReturn_SUCCESS) {
            val texturePath = pathBuffer.dataString().replace("\\", "/")
            val fullPath = "/" + modelDirectory.resolve(texturePath).normalize().joinToString("/")
            val wrapU = aiToWrapMode(mapMode[0])
            val wrapV = aiToWrapMode(mapMode[0])

            if (texturePath.startsWith("*")) {
                val embeddedTexture = loadTexture
            }

            diffuseTexture = Texture(

            )

            //diffuseTexture = Texture.fromImageResource(
            //    fullPath,
            //    wrapU,
            //    wrapV
            //)
        }

        // Add more properties like reflectivity, shininess, etc., if needed
        return Material(
            diffuseTexture,
            baseColor=Vector3f(
                color.r(),
                color.g(),
                color.b()
            ).toColor(),
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
        node: AINode,
        meshes: List<Mesh>,
        parentModelTransform: Matrix4f = Matrix4f(),
    ): ModelCacheData {
        // where each bone/model is relative to it's parent
        val localTransform = node.mTransformation().toJoml()
        // where each model is in world space
        val modelTransform = parentModelTransform.mul(localTransform, Matrix4f())

        val childrenData = List(node.mNumChildren()) { i ->
            val childNode = AINode.create(node.mChildren()!![i])
            importNode(childNode, meshes, modelTransform)
        }

        val nodeMeshes = List(node.mNumMeshes()) { i ->
            meshes[node.mMeshes()!![i]]
        }

        return ModelCacheData(
            node.mName().dataString(),
            childrenData,
            nodeMeshes
        )
    }
}