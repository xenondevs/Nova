package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bukkit.World
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.world.format.chunk.NetworkChunk
import xyz.xenondevs.nova.world.format.chunk.RegionChunk
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunk
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal class WorldDataStorage(val world: World) {
    
    private val blockRegionFolder = File(world.worldFolder, "nova_region")
    private val networkRegionFolder = File(world.worldFolder, "nova_network_region")
    
    private val blockRegionFiles = ConcurrentHashMap<Long, Deferred<RegionFile>>()
    private val networkRegionFiles = ConcurrentHashMap<Long, Deferred<NetworkRegionFile>>()
    val networkState = NetworkState(world, this)
    
    init {
        blockRegionFolder.mkdirs()
        networkRegionFolder.mkdirs()
    }
    
    suspend fun getOrLoadRegion(pos: ChunkPos): RegionFile =
        getOrLoadRegionizedFileAsync(pos, blockRegionFiles, RegionFile, blockRegionFolder, "nvr")
    
    suspend fun getOrLoadNetworkChunk(pos: ChunkPos): NetworkChunk =
        getOrLoadNetworkRegion(pos).getChunk(pos)
    
    suspend fun getOrLoadNetworkRegion(pos: ChunkPos): NetworkRegionFile =
        getOrLoadRegionizedFileAsync(pos, networkRegionFiles, NetworkRegionFile, networkRegionFolder, "nvnr")
    
    private suspend fun <C : RegionizedChunk, F : RegionizedFile<C>> getOrLoadRegionizedFileAsync(
        pos: ChunkPos,
        map: MutableMap<Long, Deferred<F>>,
        reader: RegionizedFileReader<C, F>,
        folder: File,
        extension: String,
    ): F = coroutineScope {
        val regionX = pos.x shr 5
        val regionZ = pos.z shr 5
        val regionId = getRegionId(regionX, regionZ)
        
        return@coroutineScope map.computeIfAbsent(regionId) {
            async(Dispatchers.IO) {
                val file = File(folder, "r.$regionX.$regionZ.$extension")
                reader.read(file, world, regionX, regionZ)
            }
        }.await()
    }
    
    fun getBlockChunkOrThrow(pos: ChunkPos): RegionChunk =
        blockRegionFiles[getRegionId(pos)]?.getCompleted()?.getChunk(pos)
            ?: throw IllegalStateException("Block chunk at $pos is not loaded")
    
    fun getNetworkChunkOrThrow(pos: ChunkPos): NetworkChunk =
        networkRegionFiles[getRegionId(pos)]?.getCompleted()?.getChunk(pos)
            ?: throw IllegalStateException("Network chunk at $pos is not loaded")
    
    private fun getRegionId(pos: ChunkPos): Long =
        getRegionId(pos.x shr 5, pos.z shr 5)
    
    private fun getRegionId(regionX: Int, regionZ: Int): Long =
        (regionX.toLong() shl 32) or (regionZ.toLong() and 0xFFFFFFFF)
    
    /**
     * Gets a snapshot of all loaded [TileEntities][TileEntity] in this world.
     */
    fun getTileEntities(): List<TileEntity> =
        collectFromChunks { it.getTileEntities() }
    
    /**
     * Gets a snapshot of all loaded [VanillaTileEntities][VanillaTileEntity] in this world.
     */
    fun getVanillaTileEntities(): List<VanillaTileEntity> =
        collectFromChunks { it.getVanillaTileEntities() }
    
    private fun <T> collectFromChunks(collector: (RegionChunk) -> List<T>): List<T> {
        val list = ArrayList<T>()
        for (regionFile in blockRegionFiles.values) {
            if (!regionFile.isCompleted)
                continue
            
            for (chunk in regionFile.getCompleted().chunks) {
                list += collector(chunk)
            }
        }
        
        return list
    }
    
    /**
     * Saves all Nova data related to this world.
     */
    suspend fun save(unload: Boolean = true) = withContext(Dispatchers.Default) { // TODO: save in background
        networkState.mutex.withLock { // network-related data is stored in network region files and tile-entity data (block region files)
            for ((rid, deferredRegionFile) in blockRegionFiles) {
                launch {
                    val regionFile = deferredRegionFile.await()
                    regionFile.save()
                    
                    // unload unused region files
                    if (unload && regionFile.isInactive())
                        blockRegionFiles -= rid
                }
            }
            
            for ((rid, deferredRegionFile) in networkRegionFiles) {
                launch {
                    val regionFile = deferredRegionFile.await()
                    regionFile.save()
                    
                    // network region files that don't have a corresponding block region file can be unloaded
                    // (at least most of the time, modifications to big networks may cause them to be loaded again)
                    // TODO: a better solution may be to track last access time
                    if (unload && !blockRegionFiles.containsKey(rid))
                        networkRegionFiles -= rid
                }
            }
        }
    }
    
    suspend fun disableAllChunks() {
        checkServerThread()
        for (regionFile in blockRegionFiles.values) {
            for (chunk in regionFile.await().chunks) {
                chunk.disable()
            }
        }
        
        for (regionFile in blockRegionFiles.values) {
            for (chunk in regionFile.await().chunks) {
                chunk.awaitShutdown()
            }
        }
    }
    
}