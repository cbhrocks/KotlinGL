package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.kotlingl.entity.Material
import org.kotlingl.entity.Texture
import org.kotlingl.entity.WrapMode
import org.kotlingl.entity.toColor
import org.kotlingl.math.toJoml
import org.lwjgl.BufferUtils
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.IllegalStateException
import kotlin.collections.joinToString
import kotlin.collections.plusAssign
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.extension
import kotlin.io.path.toPath


data class ModelCacheData (
    val name: String,
    val children: List<ModelCacheData>,
    val meshes: List<Mesh>,
    val modelTransform: Matrix4f,
    var skeletonHash: Int? = null
)

class ModelLoader {
    // the user defined name pointing to the file path used to import the model
    val modelLookup: MutableMap<String, String> = mutableMapOf()
    // the file name pointing to the ModelCacheData used for reconstruction.
    val modelCache: MutableMap<String, ModelCacheData> = mutableMapOf()
    val skeletonCache: MutableMap<Int, Skeleton> = mutableMapOf()
    // textures loaded from files
    val textureCache = mutableMapOf<String, Texture>()
    val animationCache = mutableMapOf<String, NodeAnimation>()


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
            modelData.children.map {buildModel(it)}.toMutableList(),
            modelData.modelTransform
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
        val url = object {}.javaClass.getResource(resourcePath)
        val path = url!!.toURI().toPath()

        val flags = when (path.extension) {
            "fbx" -> aiProcess_Triangulate or
                    aiProcess_GenSmoothNormals or
                    aiProcess_JoinIdenticalVertices or
                    aiProcess_SortByPType or
                    aiProcess_LimitBoneWeights or
                    aiProcess_ImproveCacheLocality or
                    aiProcess_RemoveRedundantMaterials or
                    //aiProcess_FlipUVs or
                    aiProcess_CalcTangentSpace or
                    aiProcess_GlobalScale or
                    aiProcess_ValidateDataStructure or
                    aiProcess_OptimizeMeshes or
                    aiProcess_OptimizeGraph or
                    aiProcess_FixInfacingNormals
            else -> aiProcess_Triangulate or
                    aiProcess_JoinIdenticalVertices or
                    aiProcess_GenSmoothNormals or
                    aiProcess_CalcTangentSpace or
                    //aiProcess_FlipUVs or
                    aiProcess_ImproveCacheLocality or
                    aiProcess_SortByPType or
                    aiProcess_ValidateDataStructure or
                    aiProcess_RemoveRedundantMaterials
        }

        val scene = aiImportFile(path.absolute().toString(), flags)
            ?: throw RuntimeException("Error loading scene: ${aiGetErrorString()}")

        val directory = Path(resourcePath).parent

        val rootNode = scene.mRootNode() ?: throw RuntimeException( "failed to load model from scene." )

        val materials = List(scene.mNumMaterials()) { i ->
            importMaterial(AIMaterial.create(scene.mMaterials()!![i]), directory, scene)
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

        val animCount = scene.mNumAnimations()
        val aiAnimations = List(animCount) { i -> AIAnimation.create(scene.mAnimations()!![i]) }
        val animations = aiAnimations.map {importAnimation(it)}.associateBy{it.name}

        val rootBoneNode = importBoneNodes(boneNames, rootNode)
        val boneMap = buildBoneNodeMap(rootBoneNode, boneNames)

        val skeleton = Skeleton(
            scene.mRootNode()?.mName()?.dataString() ?: "UnnamedSkeleton",
            rootBoneNode,
            boneMap,
            animations
        )

        val skeletonHash = skeleton.hashCode()
        skeletonCache[skeletonHash] = skeleton

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

    fun loadByteBufferFromResource(resourcePath: String): ByteBuffer {
        val stream = object {}.javaClass
            .getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        stream.use {
            val channel = Channels.newChannel(it)
            //val buffer = ByteBuffer.allocateDirect(it.available())
            val buffer = BufferUtils.createByteBuffer(it.available())
            channel.read(buffer)
            buffer.flip()
            return buffer
        }
    }

    fun importTextureFromResource(path: String): Texture {
        // Step 1: Load resource as ByteBuffer
        val imageBuffer = loadByteBufferFromResource(path)

        // Step 2: Decode image using STBImage
        //stbi_set_flip_vertically_on_load(true)
        MemoryStack.stackPush().use { stack ->
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            val channels = stack.mallocInt(1)

            val pixels = stbi_load_from_memory(imageBuffer, width, height, channels, 4)
                ?: throw RuntimeException("Failed to load image: ${stbi_failure_reason()}")

            return Texture(pixels, width[0], height[0])
        }
    }

    fun importImbeddedTexture(aiTex: AITexture): Texture {
        // Get raw image data from Assimp's embedded texture
        val isCompressed = aiTex.mHeight() == 0
        if (!isCompressed) {
            throw UnsupportedOperationException("Only compressed embedded textures are supported.")
        }

        // The width field is the size in bytes when compressed
        val dataSize = aiTex.mWidth()
        val dataBuffer = aiTex.pcDataCompressed() ?: throw IllegalStateException("No texture data!")

        // Decode using STBImage
        //stbi_set_flip_vertically_on_load(true)
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
            return Texture(decoded, width, height)
        }
    }

    fun importMaterialTexture(material: AIMaterial, type: Int, scene: AIScene, modelDirectory: Path): List<Texture> {
        val count = aiGetMaterialTextureCount(material, type)
        val textures = List<Texture>(count) {
            val pathBuffer = AIString.calloc()
            val mapping = BufferUtils.createIntBuffer(1)
            val uvIndex = BufferUtils.createIntBuffer(1)
            val blend = BufferUtils.createFloatBuffer(1)
            val op = BufferUtils.createIntBuffer(1)
            val mapMode = BufferUtils.createIntBuffer(2)
            val flags = BufferUtils.createIntBuffer(1)

            val loadSuccess = aiGetMaterialTexture(
                material,
                type,
                0,
                pathBuffer,
                mapping,
                uvIndex,
                blend,
                op,
                mapMode,
                flags
            )
            if (loadSuccess != 0) {
                throw IllegalStateException("Failed ot load texture data")
            }

            val texPath = pathBuffer.dataString()

            val texture = textureCache.getOrPut(texPath) {
                if (texPath.startsWith("*")) {
                    // Embedded texture
                    val embeddedIndex = texPath.substring(1).toInt()
                    return@getOrPut importImbeddedTexture(AITexture.create(scene.mTextures()!![embeddedIndex]))
                } else {
                    // External file
                    val texturePath = pathBuffer.dataString().replace("\\", "/")
                    val fullPath = "/" + modelDirectory.resolve(texturePath).normalize().joinToString("/")
                    return@getOrPut importTextureFromResource(fullPath)
                }
            }
            texture.wrapU = aiToWrapMode(mapMode[0])
            texture.wrapV = aiToWrapMode(mapMode[1])
            return@List texture
        }
        return textures
    }

    fun importMaterial(material: AIMaterial, modelDirectory: Path, scene: AIScene): Material {
        // get base color
        val color = AIColor4D.create()
        aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color)

        // load diffuse textures
        val diffuseTextures = importMaterialTexture(material, aiTextureType_DIFFUSE, scene, modelDirectory)

        // Add more properties like reflectivity, shininess, etc., if needed
        return Material(
            diffuseTextures.getOrNull(0),
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

    fun buildBoneNodeMap(
        boneNode: BoneNode,
        boneNames: Set<String>,
        boneMap: MutableMap<String, BoneNode> = mutableMapOf()
    ): MutableMap<String, BoneNode> {
        if (boneNode.name in boneNames) {
            boneMap[boneNode.name] = boneNode
        }
        boneNode.children.forEach { buildBoneNodeMap(it, boneNames, boneMap) }
        return boneMap
    }

    fun importBoneNodes(
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
            importBoneNodes(boneNames, childNode, globalTransform)
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

    fun importAnimation(
        animation: AIAnimation
    ): Animation {
        val nodeAnimations = List(animation.mNumChannels()) { channelIndex ->
            val nodeAnim = AINodeAnim.create(animation.mChannels()!![channelIndex])

            val nodeName = nodeAnim.mNodeName().dataString()

            val positionKeys = List(nodeAnim.mNumPositionKeys()) {
                val vec = nodeAnim.mPositionKeys()!![it].mValue()
                val time = nodeAnim.mPositionKeys()!![it].mTime()
                Keyframe(time, Vector3f(vec.x(), vec.y(), vec.z()))
            }

            val rotationKeys = List(nodeAnim.mNumRotationKeys()) {
                val quat = nodeAnim.mRotationKeys()!![it].mValue()
                val time = nodeAnim.mRotationKeys()!![it].mTime()
                Keyframe(time, Quaternionf(quat.x(), quat.y(), quat.z(), quat.w()))
            }

            val scaleKeys = List(nodeAnim.mNumScalingKeys()) {
                val vec = nodeAnim.mScalingKeys()!![it].mValue()
                val time = nodeAnim.mScalingKeys()!![it].mTime()
                Keyframe(time, Vector3f(vec.x(), vec.y(), vec.z()))
            }

            NodeAnimation(
                nodeName,
                positionKeys,
                rotationKeys,
                scaleKeys
            )
        }.associateBy { it.nodeName }

        return Animation(
            animation.mName().dataString(),
            animation.mDuration(),
            animation.mTicksPerSecond(),
            nodeAnimations
        )
    }

    fun importNode(
        node: AINode,
        meshes: List<Mesh>,
        parentModelTransform: Matrix4f = Matrix4f(),
    ): ModelCacheData {
        // where each bone/model is relative to it's parent
        val localTransform = node.mTransformation().toJoml()
        // where each model is in world space
        val modelTransform = Matrix4f(parentModelTransform).mul(localTransform, Matrix4f())
        println("${node.mName().dataString()} translation: ${modelTransform.getTranslation(Vector3f())}")
        println("${node.mName().dataString()} scale: ${modelTransform.getScale(Vector3f())}")

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
            nodeMeshes,
            localTransform
            //modelTransform,
        )
    }
}