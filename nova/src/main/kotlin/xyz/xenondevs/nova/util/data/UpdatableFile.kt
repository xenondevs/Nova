package xyz.xenondevs.nova.util.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import xyz.xenondevs.commons.collections.concurrentHashSet
import xyz.xenondevs.commons.collections.removeIf
import xyz.xenondevs.nova.DATA_FOLDER
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.resources.ResourcePath
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.io.path.createParentDirectories
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

internal object UpdatableFile {
    
    private val PLUGINS_DIR = DATA_FOLDER.parent
    private val fileHashes = ConcurrentHashMap(PermanentStorage.retrieve<Map<String, String>>(STORAGE_KEY) ?: emptyMap())
    private val extractions = concurrentHashSet<String>()
    
    suspend fun extractIdNamedFromAllAddons(
        dirName: String,
        filter: (Path) -> Boolean = { true }
    ): Map<Path, ByteArray> = withContext(Dispatchers.IO) {
        extractions += dirName
        val files = AddonBootstrapper.addons.map { addon ->
            async {
                addon.file.useZip { zip ->
                    extractUpdatableFiles(
                        zip.resolve(dirName),
                        addon.dataFolder.resolve(dirName),
                        PLUGINS_DIR,
                        fileHashes
                    ) { ResourcePath.isValidPath(it.name) && filter(it) }
                }
            }
        }.awaitAll().flatMap { it.entries }.associate { it.key to it.value }
        PermanentStorage.store<Map<String, String>>(STORAGE_KEY, fileHashes)
        files
    }
    
    suspend fun reset(wildcard: String): Int {
        val regex = WildcardUtils.toRegex(wildcard)
        var count = 0
        fileHashes.removeIf { [path, _] ->
            val matches = path.matches(regex)
            if (matches)
                count++
            matches
        }
        extractions.forEach { extractIdNamedFromAllAddons(it) }
        return count
    }
    
    fun getTrackedFilePaths(): Set<String> = fileHashes.keys
    
}

private suspend fun extractUpdatableFiles(
    fromDir: Path,
    toDir: Path,
    pluginsDir: Path,
    fileHashes: ConcurrentMap<String, String>,
    filter: (Path) -> Boolean
): Map<Path, ByteArray> = withContext(Dispatchers.IO) {
    val defaults = fromDir.walk()
        .filter(filter)
        .map { it.relativeTo(fromDir).invariantSeparatorsPathString }
        .toSet()
    val paths = defaults + toDir.walk()
        .filter(filter)
        .map { it.relativeTo(toDir).invariantSeparatorsPathString }
    
    paths.map { relativePath ->
        async {
            val from = fromDir.resolve(relativePath)
            val to = toDir.resolve(relativePath)
            val fromExists = from.exists()
            val toExists = to.exists()
            if ((fromExists && !from.isRegularFile()) || (toExists && !to.isRegularFile()) || (!fromExists && !toExists))
                return@async null
            
            val id = to.relativeTo(pluginsDir).invariantSeparatorsPathString
            val storedHash = fileHashes[id]?.decodeBase64()
            val existingData = if (toExists) to.readBytes() else null
            val existingHash = existingData?.generateMD5Hash()
            
            val data = when {
                fromExists -> when {
                    // new or explicitly reset default: write/replace
                    storedHash == null -> {
                        val data = from.readBytes()
                        to.createParentDirectories()
                        to.writeBytes(data)
                        fileHashes[id] = data.generateMD5Hash().encodeBase64()
                        data
                    }
                    
                    // user deleted it: keep as-is
                    existingData == null -> null
                    
                    // user edited it: keep as-is
                    !existingHash.contentEquals(storedHash) -> existingData
                    
                    // unedited: update if needed
                    else -> {
                        val data = from.readBytes()
                        if (!data.contentEquals(existingData))
                            to.writeBytes(data)
                        fileHashes[id] = data.generateMD5Hash().encodeBase64()
                        data
                    }
                }
                
                else -> when {
                    // user created it: keep as-is
                    storedHash == null -> existingData
                    
                    // user edited an obsolete default: keep as-is
                    !existingHash.contentEquals(storedHash) -> existingData
                    
                    // obsolete default: remove
                    else -> {
                        to.deleteExisting()
                        fileHashes -= id
                        null
                    }
                }
            }
            data?.let { to to it }
        }
    }.awaitAll().filterNotNull().toMap()
}

private fun ByteArray.generateMD5Hash(): ByteArray =
    HashUtils.getHash(this, "MD5")