package xyz.xenondevs.nova.util.data

import io.netty.buffer.Unpooled
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.inputStream

internal fun MessageDigest.update(ins: InputStream, bufferSize: Int = 4096) {
    val buffer = ByteArray(bufferSize)
    var len: Int
    while (run { len = ins.read(buffer); len } != -1) {
        update(buffer, 0, len)
    }
    ins.close()
}

internal object HashUtils {
    
    fun getFileHash(file: Path, algorithm: String): ByteArray {
        return file.inputStream().use { getHash(it, algorithm) }
    }
    
    fun getFileHash(file: File, algorithm: String): ByteArray {
        return file.inputStream().use { getHash(it, algorithm) }
    }
    
    fun getHash(inputStream: InputStream, algorithm: String): ByteArray {
        val md = MessageDigest.getInstance(algorithm)
        var len: Int
        val buffer = ByteArray(4096)
        while (run { len = inputStream.read(buffer); len } != -1) {
            md.update(buffer, 0, len)
        }
        return md.digest()
    }
    
    fun getHash(data: ByteArray, algorithm: String): ByteArray {
        val md = MessageDigest.getInstance(algorithm)
        md.update(data)
        return md.digest()
    }
    
    fun getUUID(vararg objects: Any): UUID {
        val buffer = Unpooled.buffer()
        objects.forEach { buffer.writeInt(it.hashCode()) }
        return UUID.nameUUIDFromBytes(buffer.toByteArray())
    }
    
}