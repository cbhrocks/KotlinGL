package org.kotlingl.utils

import org.lwjgl.BufferUtils
import org.w3c.dom.Document
import java.awt.image.BufferedImage
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory

object ResourceLoader {
    private fun toResourcePath(path: Path): String {
        // Ensures resource path always starts with a slash
        return "/" + path.joinToString("/") { it.toString() }
    }

    fun existsAsResource(path: Path): Boolean {
        return ResourceLoader::class.java.getResource(toResourcePath(path)) != null
    }

    fun existsAsFile(path: Path): Boolean {
        return Files.exists(path)
    }

    fun openStream(path: Path): InputStream {
        val resourcePath = toResourcePath(path)
        val resourceStream = ResourceLoader::class.java.getResourceAsStream(resourcePath)
        if (resourceStream != null) {
            println("Loaded from resource: $resourcePath")
            return resourceStream
        }

        if (Files.exists(path)) {
            println("Loaded from file: $path")
            return Files.newInputStream(path)
        }

        throw FileNotFoundException("Path not found as resource or file: $path")
    }

    fun loadByteBuffer(path: Path): ByteBuffer {
        openStream(path).use { stream ->
            val channel = Channels.newChannel(stream)
            val buffer = BufferUtils.createByteBuffer(stream.available())
            channel.read(buffer)
            buffer.flip()
            return buffer
        }
    }

    fun getEffectivePath(path: Path): Path {
        val resourcePath = toResourcePath(path)
        val resourceUrl = ResourceLoader::class.java.getResource(resourcePath)
        if (resourceUrl != null) {
            println("Resolved effective path from resource: $resourcePath")
            return Paths.get(resourceUrl.toURI())
        }

        if (Files.exists(path)) {
            println("Resolved effective path from file: $path")
            return path
        }

        throw FileNotFoundException("No resource or file found at: $path")
    }

    fun extractDirectory(path: Path): Path {
        return getEffectivePath(path).parent
    }

    fun normalizePath(path: String): String {
        return path.replace("\\", "/")
    }

    fun getResourceURL(path: String): URL {
        return ResourceLoader::class.java.getResource(normalizePath(path))
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

    fun extractDirectoryToTemp(path: Path): Path {
        val resourcePath = "/" + path.joinToString("/") // Convert to classpath-style
        val resourceUrl = ResourceLoader::class.java.getResource(resourcePath)
            ?: throw FileNotFoundException("Resource path not found: $path")

        val tempDir = Files.createTempDirectory("resourceTemp")

        if (resourceUrl.protocol == "jar") {
            // Resource is inside a JAR
            val jarPath = resourceUrl.path.substringBefore("!").removePrefix("file:")
            val jarFilePath = Paths.get(URI.create("file:$jarPath"))
            val fs = FileSystems.newFileSystem(jarFilePath, emptyMap<String, Any>())

            val rootInJar = fs.getPath(resourcePath)
            Files.walk(rootInJar).use { stream ->
                stream.filter { !Files.isDirectory(it) }.forEach { file ->
                    val relative = rootInJar.relativize(file)
                    val target = tempDir.resolve(relative.toString())
                    Files.createDirectories(target.parent)
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }

        } else {
            // Resource is on disk
            val rootPath = Paths.get(resourceUrl.toURI())
            Files.walk(rootPath).use { stream ->
                stream.filter { !Files.isDirectory(it) }.forEach { file ->
                    val relative = rootPath.relativize(file)
                    val target = tempDir.resolve(relative.toString())
                    Files.createDirectories(target.parent)
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }

        println("Extracted directory to temp: $tempDir")
        return tempDir
    }

    fun getText(path: Path): String {
        return openStream(path).reader().readText()
    }

    fun loadXmlDocument(path: Path): Document {
        val inputStream = openStream(path)
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return docBuilder.parse(inputStream)
    }

    fun loadImage(path: Path): BufferedImage {
        return ImageIO.read(openStream(path))
    }
    // Extend for loading JSON, audio, shaders, etc.
}