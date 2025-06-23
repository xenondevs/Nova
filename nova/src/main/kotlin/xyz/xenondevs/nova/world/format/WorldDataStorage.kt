package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bukkit.World
import xyz.xenondevs.nova.util.concurrent.checkServerThread

internal class WorldDataStorage(val world: World) {
    
    private val fileAccess = AsyncFileAccess()
    val blockStorage = RegionFileStorage(world, "nova_region", "nvr", fileAccess, RegionFile)
    val networkStorage = RegionFileStorage(world, "nova_network_region", "nvnr", fileAccess, NetworkRegionFile)
    val networkState = NetworkState(networkStorage)
    
    /**
     * Saves all Nova data related to this world.
     */
    suspend fun save(unload: Boolean = true) = withContext(Dispatchers.Default) {
        networkState.mutex.withLock { // network-related data is stored in network region files and tile-entity data (block region files)
            blockStorage.saveAndUnload { _, regionFile -> unload && regionFile.isInactive() }
            networkStorage.saveAndUnload { rid, _ ->
                // network region files that don't have a corresponding block region file can be unloaded
                // (at least most of the time, modifications to big networks may cause them to be loaded again)
                // TODO: a better solution may be to track last access time
                unload && !blockStorage.isLoaded(rid)
            }
        }
    }
    
    /**
     * Disables all chunks in this world and waits for completion of async tile entity tasks.
     * Then saves all region files to disk and returns once everything has been written to disk.
     */
    suspend fun shutdownAndWait() {
        checkServerThread()
        
        // disable all chunks
        blockStorage.awaitRegionizedFiles()
            .flatMap(RegionFile::chunks)
            .onEach { it.disable() }
            .onEach { it.awaitShutdown() }
        
        // save data
        save(unload = false)
        fileAccess.shutdownAndWait()
    }
    
}