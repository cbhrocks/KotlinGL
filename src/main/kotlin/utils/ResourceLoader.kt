package org.kotlingl.utils

import org.w3c.dom.Document
import java.awt.image.BufferedImage
import java.io.InputStream
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory

object ResourceLoader {
    fun normalizePath(path: String): String {
        return path.replace("\\", "/")
    }

    fun getResourceURL(path: String): URL {
        return ResourceLoader::class.java.getResource(normalizePath(path))
            ?: error("Resource not found: $path")
    }

    fun getResourceAsStream(path: String): InputStream {
        return ResourceLoader::class.java.getResourceAsStream(normalizePath(path))
            ?: error("Resource not found: $path")
    }

    fun getResourceAsTempFile(path: String): Path {
        val inputStream = ResourceLoader.javaClass.getResourceAsStream(path)
            ?: error("Resource not found: $path")

        // Copy to a temp file
        val tempFile = kotlin.io.path.createTempFile(suffix = path.substringAfterLast('.'))
        tempFile.toFile().outputStream().use { out ->
            inputStream.copyTo(out)
        }

        return tempFile
    }

    fun extractResourceDirectory(resourceRoot: String): Path {
        val rootURL = object {}.javaClass.getResource(resourceRoot)
            ?: error("Resource path not found: $resourceRoot")

        val tempDir = Files.createTempDirectory("assimpTemp")

        // Depending on packaging, you need different logic
        if (rootURL.protocol == "jar") {
            // Running from JAR
            val jarPath = rootURL.path.substringBefore("!").removePrefix("file:")
            val fs = FileSystems.newFileSystem(Paths.get(jarPath), null as ClassLoader?)
            val rootPathInJar = fs.getPath(resourceRoot)

            Files.walk(rootPathInJar).forEach { jarFile ->
                if (!Files.isDirectory(jarFile)) {
                    val relativePath = rootPathInJar.relativize(jarFile)
                    val destPath = tempDir.resolve(relativePath.toString())
                    Files.createDirectories(destPath.parent)
                    Files.copy(jarFile, destPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }

        } else {
            // Running from disk (e.g., during development)
            val rootPath = Paths.get(rootURL.toURI())
            Files.walk(rootPath).forEach { file ->
                if (!Files.isDirectory(file)) {
                    val relative = rootPath.relativize(file)
                    val dest = tempDir.resolve(relative.toString())
                    Files.createDirectories(dest.parent)
                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }

        return tempDir
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