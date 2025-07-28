package org.kotlingl.model

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.kotlingl.entity.*
import org.kotlingl.math.toJoml
import org.kotlingl.utils.ResourceLoader
import org.lwjgl.BufferUtils
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.IllegalStateException
import kotlin.collections.joinToString
import kotlin.collections.plusAssign
import kotlin.io.path.extension


data class ModelCacheData (
    val name: String,
    val nodeToMeshIndices: MutableMap<Int, List<Int>>,
    val meshes: List<Mesh>,
    val materials: List<Material>,
    val meshIndexToMaterialIndex: MutableMap<Int, Int>,
    val modelTransform: Matrix4f,
    var skeletonHash: Int
)

data class Model2DCacheData (
    val name: String,
    val material: Material,
    val animations: List<TextureAnimation>,
)

data class NodeTraversalData (
    val nodeMap: MutableMap<Int, SkeletonNode>,
    val nameToNodeId: MutableMap<String, MutableList<Int>>,
    val nodeToMeshIndices: MutableMap<Int, List<Int>>,
)

object ModelLoader {
    // the user defined name pointing to the file path used to import the model
    val modelLookup: MutableMap<String, String> = mutableMapOf()
    // the file name pointing to the ModelCacheData used for reconstruction.
    val modelCache: MutableMap<String, ModelCacheData> = mutableMapOf()
    val skeletonCache: MutableMap<Int, Skeleton> = mutableMapOf()
    // textures loaded from files
    val textureCache = mutableMapOf<String, Texture>()
    val modelCache2D = mutableMapOf<String, Model2DCacheData>()


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
            modelData.materials,
            skeletonCache.getValue(modelData.skeletonHash),
            modelData.nodeToMeshIndices,
            modelData.meshIndexToMaterialIndex,
            Matrix4f(modelData.modelTransform),
        )
    }

    fun createModel2D(lookupName: String): Model {
        val filePath = modelLookup[lookupName] ?: error("Model $lookupName hasn't been loaded.")
        val model = buildModel2D(modelCache2D.getValue(filePath))
        return model
    }

    fun buildModel2D(modelData: Model2DCacheData): Model {
        return PrimitiveFactory.createQuad(
            modelData.name,
            modelData.material
        ).apply {
            val aspectRatio = (modelData.material.diffuseTexture?.width?.toFloat() ?: 1f)/
                    (modelData.material.diffuseTexture?.height ?: 1)
            transform(scale=Vector3f(aspectRatio, 1f, 1f))
            modelData.animations.forEach {
                textureAnimator.addAnimation("quad", it)
            }
        }
    }

    fun loadModelFromResource(resourcePath: String, name: String) {
        val modelUrl = ResourceLoader.getResourceURL(resourcePath)
        val modelPath = Paths.get(modelUrl.toURI())
        val dirPath = ResourceLoader.extractResourceDirectory(resourcePath.substringBeforeLast("/"))

        val filePath = dirPath.resolve(modelPath.fileName)
        loadModel(filePath, name)
    }

    fun loadModel(filePath: Path, name: String) {
        if (modelLookup.containsKey(name)) throw IllegalArgumentException("model already exists with that name!")
        if (modelCache.containsKey(filePath.toString())) throw IllegalArgumentException("model already loaded!")
        importModel(filePath)
        modelLookup[name] = filePath.toString()
    }

    fun loadModelFromSpriteSheetAtlas(filePath: String, name: String) {
        if (modelLookup.containsKey(name)) throw IllegalArgumentException("model already exists with that name!")
        if (modelCache2D.containsKey(filePath)) throw IllegalArgumentException("model already loaded!")
        importModel2D(filePath)
        modelLookup[name] = filePath
    }

    private fun importModel(path: Path) {
        //val path = normalizePath(filepath)

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

        val scene = aiImportFile(path.toString(), flags)
            ?: throw RuntimeException("Error loading scene: ${aiGetErrorString()}")

        val directory = path.parent

        val rootNode = scene.mRootNode() ?: throw RuntimeException( "failed to load model from scene." )

        val materials = List(scene.mNumMaterials()) { i ->
            importMaterial(AIMaterial.create(scene.mMaterials()!![i]), directory, scene)
        }
        val aiMeshes = List(scene.mNumMeshes()) { i ->
            AIMesh.create(scene.mMeshes()!![i])
        }
        val meshIndexToMaterialIndex = mutableMapOf<Int, Int>()
        val meshes = aiMeshes.mapIndexed { i, it ->
            meshIndexToMaterialIndex.put(i, it.mMaterialIndex())
            importMesh(it)
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
            materials,
            meshIndexToMaterialIndex,
            Matrix4f(),
            skeletonHash
        )
        modelCacheData.skeletonHash = skeletonHash
        modelCache[path.toString()] = modelCacheData

        aiReleaseImport(scene)
    }

    fun aiToWrapMode(mode: Int): WrapMode = when (mode) {
        aiTextureMapMode_Wrap -> WrapMode.REPEAT
        aiTextureMapMode_Clamp -> WrapMode.CLAMP
        aiTextureMapMode_Mirror -> WrapMode.MIRROR
        else -> WrapMode.REPEAT
    }

    fun loadByteBufferFromFile(filePath: Path): ByteBuffer {
        val size = Files.size(filePath).toInt()
        val buffer = MemoryUtil.memAlloc(size)
        Files.newByteChannel(filePath).use { channel ->
            channel.read(buffer)
        }
        buffer.flip()
        return buffer
    }

    fun importTextureFromFile(path: Path, flip: Boolean = false): Texture {
        // Step 1: Load resource as ByteBuffer
        val imageBuffer = loadByteBufferFromFile(path)

        // Step 2: Decode image using STBImage
        stbi_set_flip_vertically_on_load(flip)
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
                    val fullPath = modelDirectory.resolve(texturePath).normalize()
                    return@getOrPut importTextureFromFile(fullPath)
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

    fun importMesh(aiMesh: AIMesh): Mesh {
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
            Vertex( position, normal, uv)
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

        return Mesh(vertices, indices, meshBones, aiMesh.mName().dataString())
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

            for (i in 0 until nodeAnim.mNumRotationKeys()) {
                val key = assimpKeys!![i].mValue()
                val time = assimpKeys[i].mTime().toFloat()
                val raw = Quaternionf(key.x(), key.y(), key.z(), key.w()).normalize()

                // Hemisphere correction
                val corrected = if (lastQuat != null && lastQuat.dot(raw) < 0f)
                    Quaternionf(raw).mul(-1f) else raw

                // Optional: skip redundant identity-like frames
                val isIdentity = corrected.angle() < 0.01f
                val isFirstOrLast = (i == 0 || i == nodeAnim.mNumRotationKeys() - 1)

                if (!isIdentity || isFirstOrLast) {
                    rotationKeys.add(Keyframe(time, corrected))
                    lastQuat = corrected
                } else {
                    // still update lastQuat to preserve continuity
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

    fun importModel2D(filePath: String) {
        // create Material for model2D
        val doc = ResourceLoader.loadXmlDocument(filePath)
        val texturePath = Paths.get(ResourceLoader.normalizePath(filePath)).parent.resolve(
                doc.documentElement.getAttribute("imagePath"))
        val texture = textureCache.getOrPut(texturePath.toString()) {
            importTextureFromFile(texturePath, true)
        }
        val material = Material(
            texture,
            baseColor = ColorRGB(0, 0, 0, 0)
        )
        val animations = importAnimationsFromAtlas(
            doc,
            texture
        )
        modelCache2D.set(filePath, Model2DCacheData(
            filePath,
            material,
            animations,
        ))
    }

    fun importAnimationsFromAtlas(
        doc: Document,
        texture: Texture,
        ticksPerSecond: Float = 3f
    ): List<TextureAnimation> {
        doc.documentElement.normalize()

        val spriteMap = mutableMapOf<String, MutableList<Pair<String, SpriteBB>>>()

        val subTextures = doc.getElementsByTagName("SubTexture")
        for (i in 0 until subTextures.length) {
            val element = subTextures.item(i) as Element
            val name = element.getAttribute("name")
            val x = element.getAttribute("x").toInt()
            // val y = texture.height - element.getAttribute("y").toInt()
            val width = element.getAttribute("width").toInt()
            val height = element.getAttribute("height").toInt()
            val y = texture.height - element.getAttribute("y").toInt() - height

            val bb = SpriteBB(
                name = name,
                min = Vector2f(x.toFloat() / texture.width, y.toFloat() / texture.height),
                max = Vector2f((x + width).toFloat()/texture.width, (y + height).toFloat()/texture.height),
                //min = Vector2f(x.toFloat() / texture.width, (y + height).toFloat() / texture.height),
                //max = Vector2f((x + width).toFloat()/texture.width, y.toFloat()/texture.height),
                pixelMin = Vector2i(x, y),
                pixelMax = Vector2i(x + width, y + height),
            )

            // Try to group based on animation name prefix
            var animName: String
            if (name.substringAfterLast("_").matches(Regex("[A-Za-z0-9]"))) {
                animName = name.substringBeforeLast('_')
            }
            else {
                animName = name
            }
            spriteMap.getOrPut(animName) { mutableListOf() }.add(name to bb)
        }

        // Convert groups into TextureAnimation
        return spriteMap.map { (name, frameList) ->
            val sortedFrames = frameList.sortedBy { it.first }
            var keyframes = sortedFrames.mapIndexed { index, (_, bb) ->
                val time = index / ticksPerSecond
                Keyframe(time, bb)
            }.toMutableList()

            // currently animations only switch frames when the time reaches the point of the keyframe. This can cause
            // animations with only 2 frames to never play the second one. We get around this by adding the first frame
            // onto the end
            if (keyframes.size <= 2) {
                keyframes.addLast(
                    Keyframe(
                        keyframes.last().time * 2,
                        keyframes[0].value
                    )
                )
            }

            TextureAnimation(
                name = name,
                duration = keyframes.lastOrNull()?.time ?: 0f,
                ticksPerSecond = ticksPerSecond,
                keyframes = keyframes
            )
        }
    }
}