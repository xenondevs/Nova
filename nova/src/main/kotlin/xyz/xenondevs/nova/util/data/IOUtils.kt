package xyz.xenondevs.nova.util.data

import xyz.xenondevs.nova.NOVA
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.math.max
import kotlin.streams.asSequence

private val ZIP_FILE = ZipFile(NOVA.pluginFile)

/**
 * Returns a list of all resources in the plugin.
 *
 * @param directory The directory the resources should be in
 */
internal fun getResources(directory: String = ""): Sequence<String> {
    return ZIP_FILE.stream().asSequence().filter {
        it.name.startsWith(directory) && !it.isDirectory && !it.name.endsWith(".class")
    }.map(ZipEntry::getName)
}

// FIXME: resource leak
internal fun getResources(file: File, directory: String = ""): Sequence<String> {
    return ZipFile(file).stream().asSequence().filter {
        it.name.startsWith(directory) && !it.isDirectory && !it.name.endsWith(".class")
    }.map(ZipEntry::getName)
}

/**
 * Searches a resource with the given [name] and returns
 * the data as a stream.
 */
internal fun getResourceAsStream(name: String): InputStream? {
    val entry = ZIP_FILE.getEntry(name) ?: return null
    return ZIP_FILE.getInputStream(entry)
}

internal fun getResourceAsStream(file: File, name: String): InputStream? {
    val zipFile = ZipFile(file)
    return zipFile.getInputStream(zipFile.getEntry(name) ?: return null)
}

internal fun hasResource(name: String): Boolean =
    ZIP_FILE.getEntry(name) != null

internal fun getResourceData(name: String): ByteArray {
    val stream = getResourceAsStream(name) ?: return byteArrayOf()
    return stream.use(InputStream::readBytes)
}

fun File.write(stream: InputStream) {
    parentFile.mkdirs()
    outputStream().use { out -> stream.use { it.transferTo(out) } }
}

fun InputStream.transferTo(output: OutputStream, amount: Int) {
    output.write(this.readNBytes(amount))
}

/**
 * Appends the given [bytes] to the file at the given [pos].
 */
internal fun RandomAccessFile.append(pos: Long, bytes: ByteArray) {
    if (length() == 0L) {
        write(bytes)
        return
    }
    var toWrite = bytes.copyOf()
    val buffer = ByteArray(max(bytes.size, 1024))
    seek(pos)
    var count = read(buffer)
    var delta: Int
    seek(pos)
    write(toWrite)
    while (count != -1) {
        toWrite = buffer.copyOfRange(0, count)
        delta = toWrite.size - bytes.size
        if (delta > 0)
            skipBytes(delta)
        count = read(buffer)
        if (count != -1)
            seek(filePointer - count)
        if (delta > 0)
            seek(filePointer - delta)
        write(toWrite)
    }
}

/**
 * Overwrites everything between [pos] and [appendAt] then moves remaining bytes after newly added [bytes]. If [bytes] is
 * too small to fill the space between [pos] and [appendAt] it will be deleted.
 */
internal fun RandomAccessFile.append(pos: Long, appendAt: Long, bytes: ByteArray) {
    if (pos >= appendAt) {
        append(pos, bytes)
        return
    }
    val distance = appendAt - pos
    if (bytes.size < distance) {
        val delta = (distance - bytes.size).toInt()
        seek(pos)
        write(bytes)
        var lastPosition = filePointer
        skipBytes(delta)
        val buffer = ByteArray(delta)
        var count = read(buffer)
        while (count != -1) {
            seek(lastPosition)
            write(buffer)
            lastPosition = filePointer
            skipBytes(buffer.size)
            count = read(buffer)
        }
        setLength(length() - delta)
    } else {
        val left = bytes.copyOfRange(0, distance.toInt())
        val right = bytes.copyOfRange(distance.toInt(), bytes.size)
        seek(pos)
        write(left)
        if (right.isNotEmpty())
            append(appendAt, right)
    }
}

internal fun DataOutputStream.writeVarInt(value: Int): Int {
    var currentValue = value
    var count = 1
    while ((currentValue and -128) != 0) {
        writeByte(((currentValue and 127) or 128))
        currentValue = currentValue ushr 7
        ++count
    }
    
    this.writeByte(currentValue)
    return count
}

internal fun DataOutputStream.writeVarLong(value: Long): Int {
    var currentValue = value
    var count = 1
    while ((currentValue and -128L) != 0.toLong()) {
        this.writeByte(((currentValue and 127) or 128).toInt())
        currentValue = currentValue ushr 7
        ++count
    }
    
    this.writeByte(currentValue.toInt())
    return count
}

internal fun DataOutputStream.writeString(string: String): Int {
    val encoded = string.toByteArray()
    val size = writeVarInt(encoded.size) + encoded.size
    write(encoded)
    return size
}

internal fun DataOutputStream.writeStringList(list: List<String>): Int {
    var size = writeVarInt(list.size)
    list.forEach { size += writeString(it) }
    return size
}

internal fun DataInputStream.readVarInt(): Int {
    var value = 0
    var currentByte: Byte
    var byteIdx = 0
    
    do {
        currentByte = readByte()
        value = value or ((currentByte.toInt() and 127) shl byteIdx++ * 7)
        check(byteIdx < 6) { "VarInt is too big" }
    } while (currentByte.countLeadingZeroBits() == 0)
    
    return value
}

internal fun DataInputStream.readVarLong(): Long {
    var value = 0L
    var currentByte: Byte
    var byteIdx = 0
    
    do {
        currentByte = readByte()
        value = value or ((currentByte.toLong() and 127) shl byteIdx++ * 7)
        check(byteIdx < 10) { "VarLong is too big" }
    } while (currentByte.countLeadingZeroBits() == 0)
    
    return value
}

internal fun DataInputStream.readString(): String {
    val size = readVarInt()
    val bytes = ByteArray(size)
    readFully(bytes)
    return String(bytes)
}

internal fun DataInputStream.readStringList(): List<String> {
    val size = readVarInt()
    val list = mutableListOf<String>()
    repeat(size) { list.add(readString()) }
    return list
}

inline fun <T> use(vararg closeable: Closeable, block: () -> T): T {
    try {
        return block()
    } finally {
        closeable.forEach {
            try {
                it.close()
            } catch (ignored: Exception) {
            }
        }
    }
}

internal fun Path.readImageDimensions(): Dimension {
    inputStream().use { 
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
    return inputStream().use(ImageIO::read)
}

internal fun Path.writeImage(image: RenderedImage, formatName: String) {
    outputStream().use { ImageIO.write(image, formatName, it) }
}

internal fun File.openZip(): Path {
    return toPath().openZip()
}

internal fun Path.openZip(): Path {
    val fs = FileSystems.newFileSystem(this)
    return fs.rootDirectories.first()
}