package xyz.xenondevs.nova.data

import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.file
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.data.decodeBase64
import xyz.xenondevs.nova.util.data.encodeBase64
import xyz.xenondevs.nova.util.data.useZip
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeBytes

private const val STORAGE_KEY = "updatable_file_hashes"
private val PLUGINS_DIR = Nova.dataFolder.parentFile.toPath()

object UpdatableFile {
    
    private val fileHashes: HashMap<String, String> = PermanentStorage.retrieve(STORAGE_KEY) { HashMap() }
    
    internal fun extractIdNamedFromAllAddons(dirName: String) {
        for (addon in AddonBootstrapper.addons) {
            addon.file.useZip { zip ->
                extractAll(
                    zip.resolve(dirName), 
                    addon.dataFolder.toPath().resolve(dirName)
                ) { ResourcePath.NON_NAMESPACED_ENTRY.matches(it.name) }
            }  
        }
    }
    
    fun extractAll(fromDir: Path, toDir: Path, filter: (Path) -> Boolean) {
        val existingPaths = HashSet<Path>()
        fromDir.walk()
            .filter { it.isRegularFile() }
            .filter(filter)
            .forEach { from ->
                val relativePath = from.relativeTo(fromDir).invariantSeparatorsPathString
                val to = toDir.resolve(relativePath)
                load(from, to)
                existingPaths.add(to)
            }
        
        // find unedited files that are no longer default and remove them
        toDir.walk()
            .filter { it.isRegularFile() }
            .filter(filter)
            .forEach { path ->
                if (path !in existingPaths
                    && path.generateMD5Hash().contentEquals(getStoredHash(path))
                ) {
                    path.deleteExisting()
                    removeStoredHash(path)
                }
            }
    }
    
    fun load(from: Path, to: Path) {
        val storedHash = getStoredHash(to)
        
        if (to.exists() && storedHash != null) {
            val existingFileHash = to.generateMD5Hash()
            // Is the file on the server unchanged?
            if (existingFileHash.contentEquals(storedHash)) {
                val newFileData = from.readBytes()
                val newFileHash = newFileData.generateMD5Hash()
                
                // Does the file need to be updated?
                if (!existingFileHash.contentEquals(newFileHash)) {
                    // Replace the file with a newer version
                    to.writeBytes(newFileData)
                    storeHash(to, newFileHash)
                }
            }
        } else if (storedHash == null) {
            // The file is not on the server and/or has never been extracted before
            to.parent.createDirectories()
            from.copyTo(to, true)
            storeHash(to)
        }
    }
    
    @DisableFun
    private fun disable() {
        PermanentStorage.store(STORAGE_KEY, fileHashes)
    }
    
    fun storeHash(file: Path, hash: ByteArray) {
        fileHashes[file.invariantSeparatorsPathString] = hash.encodeBase64()
    }
    
    fun storeHash(file: Path) {
        fileHashes[id(file)] = file.generateMD5Hash().encodeBase64()
    }
    
    fun getStoredHashString(file: Path): String? =
        fileHashes[id(file)]
    
    fun getStoredHash(file: Path): ByteArray? =
        getStoredHashString(file)?.decodeBase64()
    
    fun removeStoredHash(file: Path) {
        fileHashes -= id(file)
    }
    
    private fun id(file: Path): String =
        file.relativeTo(PLUGINS_DIR).invariantSeparatorsPathString
    
    private fun Path.generateMD5Hash(): ByteArray =
        HashUtils.getFileHash(this, "MD5")
    
    private fun ByteArray.generateMD5Hash(): ByteArray =
        HashUtils.getHash(this, "MD5")
    
}