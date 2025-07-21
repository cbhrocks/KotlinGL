package org.kotlingl.utils

import org.w3c.dom.Document
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory

object ResourceLoader {
    fun normalizePath(path: String): String {
        return path.replace("\\", "/")
    }

    fun getResourceAsStream(path: String): InputStream {
        return ResourceLoader::class.java.getResourceAsStream(normalizePath(path))
            ?: error("Resource not found: $path")
    }

    fun getText(path: String): String {
        return getResourceAsStream(path).reader().readText()
    }

    fun loadXmlDocument(path: String): Document {
        val inputStream = getResourceAsStream(path)
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return docBuilder.parse(inputStream)
    }

    fun loadImage(path: String): BufferedImage {
        return ImageIO.read(getResourceAsStream(path))
    }

    // Extend for loading JSON, audio, shaders, etc.
}