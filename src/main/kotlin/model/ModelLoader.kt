package org.kotlingl.model

import java.nio.file.Paths


class ModelLoader {
    val modelCache: MutableMap<String, Model> = mutableMapOf()
    val skeletonCache: MutableMap<>

    private fun normalizePath(path: String): String =
        Paths.get(path).normalize().toString().replace('\\', '/')

    fun loadModel(filePath: String, name: String? = null): Model {
        val key = name ?: normalizePath(filePath)

        return modelCache.getOrPut(key) {
            val loadedModel = importModel(filePath)
            modelCache[key] = loadedModel
            loadedModel
        }
    }

    fun getModel(name: String): Model? {
        return modelCache[name]
    }

    private fun importModel(filepath: String): Model {
        val path = normalizePath(filepath)
    }
}