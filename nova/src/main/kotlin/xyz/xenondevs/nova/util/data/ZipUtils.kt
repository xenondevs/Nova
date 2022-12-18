package xyz.xenondevs.nova.util.data

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo

val FileHeader.path: Path
    get() = Path(fileName)

val FileHeader.fileExtension: String
    get() = fileName.substringAfterLast('.').lowercase()

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

fun ZipFile.extractFile(fileHeader: FileHeader, destination: Path, write: ((FileHeader, InputStream, OutputStream) -> Unit)? = null) {
    destination.parent.createDirectories()
    getInputStream(fileHeader).use { ins ->
        destination.outputStream().use { out ->
            if (write != null) {
                write(fileHeader, ins, out)
            } else {
                ins.transferTo(out)
            }
        }
    }
}

fun ZipFile.extractDirectory(
    fileHeader: FileHeader,
    destination: Path,
    filter: ((FileHeader, String) -> Boolean)? = null,
    write: ((FileHeader, InputStream, OutputStream) -> Unit)? = null
) {
    fileHeaders.asSequence()
        .filter { !it.isDirectory && it.fileName.startsWith(fileHeader.fileName) }
        .forEach {
            val relPath = Path(it.fileName).relativeTo(Path(fileHeader.fileName)).invariantSeparatorsPathString
            
            if (filter == null || filter(it, relPath)) {
                extractFile(it, destination.resolve(relPath), write)
            }
        }
}

fun ZipFile.extractDirectory(
    path: String,
    destination: Path,
    filter: ((FileHeader, String) -> Boolean)? = null,
    write: ((FileHeader, InputStream, OutputStream) -> Unit)? = null
) {
    extractDirectory(getFileHeader(path)!!, destination, filter, write)
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

fun ZipFile.walk(directory: FileHeader, includeDirectories: Boolean = true): Sequence<FileHeader> {
    require(directory.isDirectory) { "File header is not a directory" }
    
    return fileHeaders.asSequence()
        .filter { it.fileName.startsWith(directory.fileName) && it != directory }
        .filter { includeDirectories || !it.isDirectory }
}