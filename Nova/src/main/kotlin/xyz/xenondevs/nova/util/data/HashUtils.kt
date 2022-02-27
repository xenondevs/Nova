package xyz.xenondevs.nova.util.data

import java.io.File
import java.security.MessageDigest

object HashUtils {
    
    fun getFileHash(file: File, algorithm: String): ByteArray {
        val inputStream = file.inputStream()
        val md = MessageDigest.getInstance(algorithm)
        var len: Int
        val buffer = ByteArray(4096)
        while (run { len = inputStream.read(buffer); len } != -1) {
            md.update(buffer, 0, len)
        }
        inputStream.close()
        return md.digest()
    }
    
    fun getHash(data: ByteArray, algorithm: String): ByteArray {
        val md = MessageDigest.getInstance(algorithm)
        md.update(data)
        return md.digest()
    }
    
}