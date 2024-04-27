package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.bukkit.World
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.chunk.NetworkChunk
import xyz.xenondevs.nova.world.format.chunk.RegionChunk
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunk
import java.io.File
import java.util.concurrent.ConcurrentHashMap

// TODO: RegionizedFile unloading
internal class WorldDataStorage(val world: World) {
    
    private val blockRegionFolder = File(world.worldFolder, "nova_region")
    private val networkRegionFolder = File(world.worldFolder, "nova_network_region")
    val networkFolder = File(world.worldFolder, "nova_network")
    
    private val blockRegionFiles = ConcurrentHashMap<Long, Deferred<RegionFile>>()
    private val networkRegionFiles = ConcurrentHashMap<Long, Deferred<NetworkRegionFile>>()
    val networkState = NetworkState(world, this)
    
    init {
        blockRegionFolder.mkdirs()
        networkRegionFolder.mkdirs()
        networkFolder.mkdirs()
    }
    
    @Suppress("DeferredResultUnused")
    suspend fun loadAsync(pos: ChunkPos) {
        getOrLoadNetworkRegionAsync(pos)
        getOrLoadRegionAsync(pos) 
    }
    
    suspend fun getOrLoadRegion(pos: ChunkPos): RegionFile =
        getOrLoadRegionAsync(pos).await()
    
    private suspend fun getOrLoadRegionAsync(pos: ChunkPos): Deferred<RegionFile> =
        getOrLoadRegionizedFileAsync(pos, blockRegionFiles, RegionFile, blockRegionFolder, "nvr")
    
    suspend fun getOrLoadNetworkRegion(pos: ChunkPos): NetworkRegionFile =
        getOrLoadNetworkRegionAsync(pos).await()
    
    suspend fun getOrLoadNetworkChunk(pos: ChunkPos): NetworkChunk =
        getOrLoadNetworkRegion(pos).getChunk(pos)
    
    private suspend fun getOrLoadNetworkRegionAsync(pos: ChunkPos): Deferred<NetworkRegionFile> =
        getOrLoadRegionizedFileAsync(pos, networkRegionFiles, NetworkRegionFile, networkRegionFolder, "nvnr")
    
    private suspend fun <C : RegionizedChunk, F : RegionizedFile<C>> getOrLoadRegionizedFileAsync(
        pos: ChunkPos,
        map: MutableMap<Long, Deferred<F>>,
        reader: RegionizedFileReader<C, F>,
        folder: File,
        extension: String,
    ): Deferred<F> = coroutineScope {
        val regionX = pos.x shr 5
        val regionZ = pos.z shr 5
        val regionId = getRegionId(regionX, regionZ)
        
        return@coroutineScope map.computeIfAbsent(regionId) {
            async(Dispatchers.IO) {
                val file = File(folder, "r.$regionX.$regionZ.$extension")
                reader.read(file, world, regionX, regionZ)
            }
        }
    }
    
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
    suspend fun getTileEntities(): List<TileEntity> =
        collectFromChunks { it.getTileEntities() }
    
    /**
     * Gets a snapshot of all loaded [VanillaTileEntities][VanillaTileEntity] in this world.
     */
    suspend fun getVanillaTileEntities(): List<VanillaTileEntity> =
        collectFromChunks { it.getVanillaTileEntities() }
    
    private suspend fun <T> collectFromChunks(collector: (RegionChunk) -> List<T>): List<T> = coroutineScope {
        val list = ArrayList<T>()
        for (regionFile in blockRegionFiles.values) {
            for (chunk in regionFile.await().chunks) {
                list += collector(chunk)
            }
        }
        
        return@coroutineScope list
    }
    
    /**
     * Saves all Nova data related to this world.
     */
    suspend fun save() = coroutineScope { // TODO: save in background
        LOGGER.info(
            "Saving ${world.name} (" +
                "${blockRegionFiles.size} region files, " +
                "${networkRegionFiles.size} network region files)"
        )
        
        for (regionFile in blockRegionFiles.values) {
            launch { regionFile.await().save() }
        }
        
        networkState.mutex.withLock {
            for (networkFile in networkRegionFiles.values) {
                launch { networkFile.await().save() }
            }
            networkState.save(this)
        }
    }
    
    suspend fun disableAllChunks() {
        for (regionFile in blockRegionFiles.values) {
            for (chunk in regionFile.await().chunks) { // TODO potential enable / disable race condition
                chunk.disable()
            }
        }
    }
    
}