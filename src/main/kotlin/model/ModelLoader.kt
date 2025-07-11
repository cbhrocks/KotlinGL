package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.kotlingl.entity.Material
import org.kotlingl.entity.Texture
import org.kotlingl.entity.WrapMode
import org.kotlingl.entity.toColor
import org.kotlingl.math.EPSILON
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
    val nodeToMeshIndices: MutableMap<Int, List<Int>>,
    val meshes: List<Mesh>,
    val modelTransform: Matrix4f,
    var skeletonHash: Int
)

data class NodeTraversalData (
    val nodeMap: MutableMap<Int, SkeletonNode>,
    val nameToNodeId: MutableMap<String, MutableList<Int>>,
    val nodeToMeshIndices: MutableMap<Int, List<Int>>,
)

class ModelLoader {
    // the user defined name pointing to the file path used to import the model
    val modelLookup: MutableMap<String, String> = mutableMapOf()
    // the file name pointing to the ModelCacheData used for reconstruction.
    val modelCache: MutableMap<String, ModelCacheData> = mutableMapOf()
    val skeletonCache: MutableMap<Int, Skeleton> = mutableMapOf()
    // textures loaded from files
    val textureCache = mutableMapOf<String, Texture>()


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
            skeletonCache.getValue(modelData.skeletonHash),
            modelData.nodeToMeshIndices,
            Matrix4f(modelData.modelTransform)
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
                    //aiProcess_LimitBoneWeights or
                    aiProcess_ImproveCacheLocality or
                    aiProcess_RemoveRedundantMaterials or
                    aiProcess_FlipUVs or
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
                it.mName().dataString()
            }
        }.flatten().toSet()

        val nodeData = walkNodes(boneNames, rootNode)

        val inverseBindPoseMap = meshes.map { mesh ->
            mesh.bones.associate {
                nodeData.nameToNodeId.getValue(it.name).first() to it.offsetMatrix
            }
        }.reduce { acc, map -> acc + map }

        val animCount = scene.mNumAnimations()
        val aiAnimations = List(animCount) { i -> AIAnimation.create(scene.mAnimations()!![i]) }
        val animations = aiAnimations.map { importAnimation(it, nodeData.nameToNodeId ) }.associateBy{it.name}

        val skeleton = Skeleton(
            scene.mRootNode()?.mName()?.dataString() ?: "UnnamedSkeleton",
            0, // root node should always be ID 0.
            nodeData.nodeMap,
            inverseBindPoseMap,
            animations,
        )

        val skeletonHash = skeleton.hashCode()
        skeletonCache[skeletonHash] = skeleton

        val modelCacheData = ModelCacheData(
            rootNode.mName().dataString(),
            nodeData.nodeToMeshIndices,
            meshes,
            Matrix4f(),
            skeletonHash
        )
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

        val materialName = AIString.calloc()
        aiGetMaterialString(material, AI_MATKEY_NAME, 0, 0, materialName)
        val nameString = materialName.dataString()
        materialName.free()

        // Add more properties like reflectivity, shininess, etc., if needed
        return Material(
            diffuseTextures.getOrNull(0),
            baseColor=Vector3f(
                color.r(),
                color.g(),
                color.b()
            ).toColor(),
            name=nameString
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
        println(aiMesh.mNumBones())

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

        return Mesh(vertices, indices, material, meshBones, aiMesh.mName().dataString())
    }

    fun walkNodes(
        boneNames: Set<String>,
        rootNode: AINode,
    ): NodeTraversalData {
        // global data
        val nodeMap: MutableMap<Int, SkeletonNode> = mutableMapOf()
        val nodeToMeshIndices: MutableMap<Int, List<Int>> = mutableMapOf()
        val nameToNodeId: MutableMap<String, MutableList<Int>> = mutableMapOf()

        val stack = ArrayDeque<AINode>().apply { addLast(rootNode) }

        var currentId = 0
        var highestId = 1
        while (!stack.isEmpty()) {
            val node = stack.removeFirst()

            // skeleton node data
            val name = node.mName().dataString()
            val nodeId = currentId
            // where each bone/model is relative to it's parent
            val localTransform = node.mTransformation().toJoml()
            val isBone = boneNames.contains(name)

            // global structs with node data
            nodeToMeshIndices.put(nodeId, List(node.mNumMeshes()) { i ->
                node.mMeshes()!!.get(i)
            })
            nameToNodeId.getOrPut(name) {
                mutableListOf()
            }.add(nodeId)

            val childNodeIds: MutableSet<Int> = mutableSetOf()
            for (i in 0 until node.mNumChildren()) {
                childNodeIds.add(highestId)
                stack.addLast(AINode.create(node.mChildren()!![i]))
                highestId++
            }

            val skeletonNode = SkeletonNode(
                nodeId,
                name,
                localTransform,
                childNodeIds.toList(),
                isBone,
            )
            nodeMap.put(nodeId, skeletonNode)
            currentId++
        }

        return NodeTraversalData(
            nodeMap,
            nameToNodeId,
            nodeToMeshIndices,
        )
    }

    fun importAnimation(
        animation: AIAnimation,
        nameToNodeId: MutableMap<String, MutableList<Int>>
    ): Animation {
        val nodeAnimations = List(animation.mNumChannels()) { channelIndex ->
            val nodeAnim = AINodeAnim.create(animation.mChannels()!![channelIndex])

            val nodeName = nodeAnim.mNodeName().dataString()
            val mappedNodeIds = nameToNodeId.getValue(nodeName)
            require(mappedNodeIds.size == 1) {
                "Cannot have an animation that references multiple nodes with the same name! $nodeName"
            }

            val positionKeys = List(nodeAnim.mNumPositionKeys()) {
                val vec = nodeAnim.mPositionKeys()!![it].mValue()
                val time = nodeAnim.mPositionKeys()!![it].mTime().toFloat()
                Keyframe(time, Vector3f(vec.x(), vec.y(), vec.z()))
            }

            val assimpKeys = nodeAnim.mRotationKeys()
            val rotationKeys = mutableListOf<Keyframe<Quaternionf>>()
            var lastQuat: Quaternionf? = null
            // val identity = Quaternionf().identity()

            for (it in 0 until nodeAnim.mNumRotationKeys()) {
                val key = assimpKeys!![it].mValue()
                val time = assimpKeys[it].mTime().toFloat()
                val q = Quaternionf(key.x(), key.y(), key.z(), key.w())

                // flip hemisphere if needed
                val corrected = if (lastQuat != null && lastQuat.dot(q) < 0f) {
                    Quaternionf(-q.x, -q.y, -q.z, -q.w).normalize()
                } else { q.normalize() }

                // Check if this is an unwanted identity keyframe
                val isIdentity = corrected.angle() < 0.01f
                val isFirstOrLast = (it == 0 || it == nodeAnim.mNumRotationKeys() - 1)

                if (!isIdentity || isFirstOrLast) {
                    rotationKeys.add(Keyframe(time, corrected))
                    lastQuat = corrected
                }
            }

            for (i in 1 until rotationKeys.size) {
                val a = rotationKeys[i - 1].value
                val b = rotationKeys[i].value
                val dot = a.dot(b)
                if (dot < 0f) {
                    println("⚠️ Hemisphere flip detected between key $i-1 and $i with dot = $dot")
                }
            }

            if (animation.mName().dataString() == "walk" && nodeName == "root") {
                for ((i, key) in rotationKeys.withIndex()) {
                    println("[$i] t=${key.time} rot=${key.value}")
                }
                println()
            }

            // val rotationKeys = List(nodeAnim.mNumRotationKeys()) {
            //     val quat = nodeAnim.mRotationKeys()!![it].mValue()
            //     val time = nodeAnim.mRotationKeys()!![it].mTime().toFloat()
            //     Keyframe(time, Quaternionf(quat.x(), quat.y(), quat.z(), quat.w()).normalize())
            // }

            val scaleKeys = List(nodeAnim.mNumScalingKeys()) {
                val vec = nodeAnim.mScalingKeys()!![it].mValue()
                val time = nodeAnim.mScalingKeys()!![it].mTime().toFloat()
                Keyframe(time, Vector3f(vec.x(), vec.y(), vec.z()))
            }

            NodeAnimation(
                mappedNodeIds.first(),
                positionKeys,
                rotationKeys,
                scaleKeys
            )
        }.associateBy { it.nodeId }

        return Animation(
            animation.mName().dataString(),
            animation.mDuration().toFloat(),
            animation.mTicksPerSecond().toFloat(),
            nodeAnimations
        )
    }
}