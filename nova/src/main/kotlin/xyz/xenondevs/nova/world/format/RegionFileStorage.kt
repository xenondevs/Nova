package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.bukkit.World
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunk
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

internal class RegionFileStorage<C : RegionizedChunk, F : RegionizedFile<C>>(
    private val world: World,
    private val folder: String,
    private val fileExt: String,
    private val fileAccess: AsyncFileAccess,
    private val reader: RegionizedFileReader<C, F>
) {
    
    private val regionFiles = ConcurrentHashMap<Long, Deferred<F>>()
    
    init {
        world.worldFolder.resolve(folder).mkdirs()
    }
    
    /**
     * Gets or loads the regionized file [F] for [pos].
     */
    suspend fun getOrLoadRegionizedFile(pos: ChunkPos): F = coroutineScope {
        val regionX = pos.x shr 5
        val regionZ = pos.z shr 5
        val regionId = getRegionId(regionX, regionZ)
        
        return@coroutineScope regionFiles.computeIfAbsent(regionId) {
            async {
                val file = getRegionFile(regionX, regionZ)
                try {
                    val byteReader = fileAccess.read(file)?.let(ByteReader::fromByteArray)
                    reader.read(byteReader, world, regionX, regionZ)
                } catch (e: Exception) {
                    throw Exception("Failed to read region file $file", e)
                }
            }
        }.await()
    }
    
    /**
     * Gets or loads the regionized chunk [C] for [pos].
     */
    suspend fun getOrLoadRegionizedChunk(pos: ChunkPos): C =
        getOrLoadRegionizedFile(pos).getChunk(pos)
    
    /**
     * Gets the regionized chunk [C] for [pos], or throws an exception if it is not loaded.
     */
    fun getRegionizedChunkOrThrow(pos: ChunkPos): C =
        regionFiles[getRegionId(pos)]?.getCompleted()?.getChunk(pos)
            ?: throw IllegalStateException("Regionized chunk at $pos is not loaded")
    
    /**
     * Gets the regionized chunk [C] for [pos], or returns `null` if it is not loaded.
     */
    fun getRegionizedChunkOrNull(pos: ChunkPos): C? {
        val deferred = regionFiles[getRegionId(pos)]
        if (deferred == null || !deferred.isCompleted)
            return null
        return deferred.getCompleted().getChunk(pos)
    }
    
    /**
     * Collects [T] from all loaded regionized chunks by invoking [collector] on each of them.
     */
    fun <T> collectFromChunks(collector: (C) -> List<T>): List<T> {
        val list = ArrayList<T>()
        for (regionFile in regionFiles.values) {
            if (!regionFile.isCompleted)
                continue
            
            for (chunk in regionFile.getCompleted().chunks) {
                list += collector(chunk)
            }
        }
        
        return list
    }
    
    /**
     * Checks whether the regionized file with [regionId] is loaded or being loaded.
     */
    fun isLoaded(regionId: Long): Boolean =
        regionFiles.containsKey(regionId)
    
    /**
     * Saves all loaded region files to disk and unloads them if the [unloadCondition] is met.
     */
    suspend fun saveAndUnload(unloadCondition: (Long, F) -> Boolean) = coroutineScope {
        for ((rid, deferredRegionFile) in regionFiles) {
            launch {
                val regionFile = deferredRegionFile.await()
                val bin = regionFile.save()
                fileAccess.write(getRegionFile(rid), bin)
                
                // unload
                if (unloadCondition(rid, regionFile))
                    regionFiles -= rid
            }
        }
    }
    
    /**
     * Awaits the load of all regionized files and returns a snapshot of all loaded regionized files.
     */
    suspend fun awaitRegionizedFiles(): List<F> {
        return regionFiles.values.awaitAll()
    }
    
    private fun getRegionId(pos: ChunkPos): Long =
        getRegionId(pos.x shr 5, pos.z shr 5)
    
    private fun getRegionId(regionX: Int, regionZ: Int): Long =
        (regionX.toLong() shl 32) or (regionZ.toLong() and 0xFFFFFFFF)
    
    private fun getRegionFile(id: Long): Path {
        val regionX = id shr 32
        val regionZ = id and 0xFFFFFFFF
        return getRegionFile(regionX.toInt(), regionZ.toInt())
    }
    
    private fun getRegionFile(regionX: Int, regionZ: Int): Path =
        world.worldFolder.toPath().resolve("$folder/r.$regionX.$regionZ.$fileExt")
    
}