package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bukkit.World
import xyz.xenondevs.nova.util.concurrent.checkServerThread

internal class WorldDataStorage(val world: World) {
    
    private val fileAccess = AsyncFileAccess()
    val networkStorage = RegionFileStorage(world, "nova_network_region", "nvnr", fileAccess, NetworkRegionFile)
    val networkState = NetworkState(networkStorage)
    
    /**
     * Saves all Nova data related to this world.
     */
    suspend fun save(unload: Boolean = true) = withContext(Dispatchers.Default) {
        networkState.mutex.withLock { // network-related data is stored in network region files and tile-entity data (block region files)
            networkStorage.saveAndUnload { _, f ->
                if (!unload)
                    return@saveAndUnload false
                
                // unload if network region file was not accessed since the last time it was saved
                var accessedSinceLastSave = false
                for (chunk in f.chunks) {
                    if (chunk.accessedSinceLastSave)
                        accessedSinceLastSave = true
                    chunk.accessedSinceLastSave = false
                }
                !accessedSinceLastSave
            }
        }
    }
    
    /**
     * Disables all chunks in this world and waits for completion of async tile entity tasks.
     * Then saves all region files to disk and returns once everything has been written to disk.
     */
    suspend fun shutdownAndWait() {
        checkServerThread()
        
        // save data
        save(unload = false)
        fileAccess.shutdownAndWait()
    }
    
}