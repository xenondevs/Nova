package xyz.xenondevs.nova.util.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

fun InputStream.transferTo(output: OutputStream, amount: Int) {
    output.write(this.readNBytes(amount))
}

inline fun <T> use(vararg closeable: Closeable, block: () -> T): T {
    try {
        return block()
    } finally {
        closeable.forEach {
            try {
                it.close()
            } catch (_: Exception) {
            }
        }
    }
}

internal fun Path.readImageDimensions(): Dimension {
    inputStream().buffered().use {
        val imageIn = ImageIO.createImageInputStream(it)
        val reader = ImageIO.getImageReadersBySuffix(extension).next()
        try {
            reader.input = imageIn
            return Dimension(reader.getWidth(0), reader.getHeight(0))
        } finally {
            reader.dispose()
        }
    }
}

internal fun Path.readImage(): BufferedImage {
    return inputStream().buffered().use(ImageIO::read)
}

internal fun Path.writeImage(image: RenderedImage, formatName: String) {
    outputStream().buffered().use { ImageIO.write(image, formatName, it) }
}

internal fun <T> File.useZip(create: Boolean = false, run: (Path) -> T): T =
    toPath().useZip(create, run)

internal inline fun <T> Path.useZip(create: Boolean = false, run: (Path) -> T): T {
    val env: Map<String, Any> = if (create) mapOf("create" to true) else emptyMap()
    return FileSystems.newFileSystem(this, env).use { run(it.rootDirectories.first()) }
}

/**
 * Decodes [value] from JSON via [json] as [T] from the file.
 */
@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T> Path.readJson(json: Json = Json): T {
    return inputStream().use { json.decodeFromStream(it) }
}

/**
 * Encodes [value] to JSON via [json] as [T] and writes it to the file.
 */
@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T> Path.writeJson(value: T, json: Json = Json) {
    outputStream().use { json.encodeToStream(value, it) }
}