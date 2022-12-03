package xyz.xenondevs.nova.util.data

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo

val FileHeader.path: Path
    get() = Path(fileName)

fun ZipFile.getFileHeader(parent: FileHeader, path: String): FileHeader? =
    getFileHeader(parent.fileName + path)

fun ZipFile.getFileHeader(parent: String, path: String): FileHeader? =
    getFileHeader(parent.removeSuffix("/") + "/" + path)

operator fun ZipFile.get(path: String): FileHeader? =
    getFileHeader(path)

operator fun ZipFile.get(parent: FileHeader, path: String): FileHeader? =
    getFileHeader(parent, path)

operator fun ZipFile.get(parent: String, path: String): FileHeader? =
    getFileHeader(parent, path)

fun ZipFile.extractFile(fileHeader: FileHeader, destination: Path) {
    destination.parent.createDirectories()
    getInputStream(fileHeader).use { ins -> destination.outputStream().use { out -> ins.copyTo(out) } }
}

fun ZipFile.extractDirectory(fileHeader: FileHeader, destination: Path, filter: ((FileHeader, String) -> Boolean)? = null) {
    fileHeaders.asSequence()
        .filter { !it.isDirectory && it.fileName.startsWith(fileHeader.fileName) }
        .forEach { 
            val relPath = Path(it.fileName).relativeTo(Path(fileHeader.fileName)).invariantSeparatorsPathString
            
            if (filter == null || filter(it, relPath)) {
                extractFile(it, destination.resolve(relPath))
            }
        }
}

fun ZipFile.extractDirectory(path: String, destination: Path, filter: ((FileHeader, String) -> Boolean)? = null) {
    extractDirectory(getFileHeader(path)!!, destination, filter)
}

fun ZipFile.extractAll(destination: Path) {
    fileHeaders.asSequence().forEach { 
        val path = destination.resolve(it.fileName)
        if (it.isDirectory) {
            path.createDirectories()
        } else {
            extractFile(it, path)
        }
    }
}

fun ZipFile.listFileHeaders(directory: FileHeader): Sequence<FileHeader> {
    require(directory.isDirectory) { "File header is not a directory" }
    return fileHeaders.asSequence().filter { it.fileName.startsWith(directory.fileName) }
}

fun ZipFile.getInputStream(path: String): InputStream? {
    return getInputStream(getFileHeader(path))
}