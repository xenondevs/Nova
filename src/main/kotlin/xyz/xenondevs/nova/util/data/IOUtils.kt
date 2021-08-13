package xyz.xenondevs.nova.util.data

import xyz.xenondevs.nova.NOVA
import java.io.InputStream
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private val ZIP_FILE = ZipFile(NOVA.pluginFile)

/**
 * Returns a list of all resources in the plugin.
 *
 * @param directory The directory the resources should be in
 */
fun getResources(directory: String = ""): List<String> {
    return ZIP_FILE.stream().filter {
        it.name.startsWith(directory) && !it.isDirectory && !it.name.endsWith(".class")
    }.map(ZipEntry::getName).collect(Collectors.toList())
}

/**
 * Searches a resource with the given [name] and returns
 * the data as a stream.
 */
fun getResourceAsStream(name: String): InputStream? {
    val entry = ZIP_FILE.getEntry(name) ?: return null
    return ZIP_FILE.getInputStream(entry)
}

fun getResourceData(name: String): ByteArray {
    val stream = getResourceAsStream(name) ?: return byteArrayOf()
    return stream.use(InputStream::readBytes)
}