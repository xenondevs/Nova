package xyz.xenondevs.nova.data

import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.data.decodeWithBase64
import xyz.xenondevs.nova.util.data.encodeWithBase64
import xyz.xenondevs.nova.util.data.write
import java.io.File
import java.io.InputStream

object UpdatableFile {
    
    private val fileHashes: HashMap<String, String> = PermanentStorage.retrieve("updatableFileHashes") { HashMap() }
    
    init {
        NOVA.disableHandlers += { PermanentStorage.store("updatableFileHashes", fileHashes) }
    }
    
    fun load(file: File, getStream: () -> InputStream) {
        val storedHash = getStoredHash(file)
        
        if (file.exists() && storedHash != null) {
            val existingFileHash = file.getMD5Hash()
            // Is the file on the server unchanged?
            if (existingFileHash.contentEquals(storedHash)) {
                val newFileData = getStream().use { it.readAllBytes() }
                val newFileHash = newFileData.getMD5Hash()
                
                // Does the file need to be updated?
                if (!existingFileHash.contentEquals(newFileHash)) {
                    // Replace the file with a newer version
                    file.write(getStream())
                    // Store the new hash
                    fileHashes[file.absolutePath] = newFileHash.encodeWithBase64()
                }
            }
        } else if (storedHash == null) {
            // The file is not on the server and has also never been extracted before
            
            // Write the file
            file.write(getStream())
            // Store the hash
            fileHashes[file.absolutePath] = file.getMD5Hash().encodeWithBase64()
        }
    }
    
    fun getStoredHashString(file: File): String? =
        fileHashes[file.absolutePath]
    
    fun getStoredHash(file: File): ByteArray? =
        getStoredHashString(file)?.decodeWithBase64()
    
    fun removeStoredHash(file: File) =
        fileHashes.remove(file.absolutePath)
    
    private fun File.getMD5Hash(): ByteArray =
        HashUtils.getFileHash(this, "MD5")
    
    private fun ByteArray.getMD5Hash(): ByteArray =
        HashUtils.getHash(this, "MD5")
    
}