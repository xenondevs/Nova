package xyz.xenondevs.nova.util.data

import xyz.xenondevs.nova.NOVA
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.max
import kotlin.streams.asSequence

private val ZIP_FILE = ZipFile(NOVA.pluginFile)

/**
 * Returns a list of all resources in the plugin.
 *
 * @param directory The directory the resources should be in
 */
fun getResources(directory: String = ""): Sequence<String> {
    return ZIP_FILE.stream().asSequence().filter {
        it.name.startsWith(directory) && !it.isDirectory && !it.name.endsWith(".class")
    }.map(ZipEntry::getName)
}

fun getResources(file: File, directory: String = ""): Sequence<String> {
    return ZipFile(file).stream().asSequence().filter {
        it.name.startsWith(directory) && !it.isDirectory && !it.name.endsWith(".class")
    }.map(ZipEntry::getName)
}

/**
 * Searches a resource with the given [name] and returns
 * the data as a stream.
 */
fun getResourceAsStream(name: String): InputStream? {
    val entry = ZIP_FILE.getEntry(name) ?: return null
    return ZIP_FILE.getInputStream(entry)
}

fun getResourceAsStream(file: File, name: String): InputStream? {
    val zipFile = ZipFile(file)
    return zipFile.getInputStream(zipFile.getEntry(name) ?: return null)
}

fun hasResource(name: String): Boolean =
    ZIP_FILE.getEntry(name) != null

fun getResourceData(name: String): ByteArray {
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
fun RandomAccessFile.append(pos: Long, bytes: ByteArray) {
    if (length() == 0L)
        write(bytes)
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
fun RandomAccessFile.append(pos: Long, appendAt: Long, bytes: ByteArray) {
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

fun RandomAccessFile.readString(): String {
    val bytes = ByteArray(readUnsignedShort())
    read(bytes)
    return bytes.decodeToString()
}

fun RandomAccessFile.readStringList(): List<String> {
    return Array(readInt()) { readString() }.asList()
}