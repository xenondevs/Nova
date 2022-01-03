package xyz.xenondevs.nova.tileentity

import org.bukkit.Bukkit
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.util.removeIf
import xyz.xenondevs.nova.world.ChunkPos
import java.util.*

object ChunkLoadManager {
    
    private val forceLoadedChunks = PermanentStorage.retrieve("forceLoadedChunks") { HashMap<ChunkPos, HashSet<UUID>>() }
    
    fun init() {
        LOGGER.info("Initializing ChunkLoadManager")
        
        Bukkit.getWorlds().flatMap { it.forceLoadedChunks }.forEach { it.isForceLoaded = false }
        forceLoadedChunks.removeIf { it.key.chunk?.apply { isForceLoaded = true } == null }
        
        NOVA.disableHandlers += {
            if (!NOVA.isUninstalled)
                PermanentStorage.store("forceLoadedChunks", forceLoadedChunks)
        }
    }
    
    fun submitChunkLoadRequest(chunk: ChunkPos, uuid: UUID) {
        val requesterSet = getChunkLoaderSet(chunk)
        if (requesterSet.isEmpty()) chunk.chunk!!.isForceLoaded = true
        requesterSet.add(uuid)
    }
    
    fun revokeChunkLoadRequest(chunk: ChunkPos, uuid: UUID) {
        val requesterSet = getChunkLoaderSet(chunk)
        requesterSet.remove(uuid)
        if (requesterSet.isEmpty()) {
            chunk.chunk?.isForceLoaded = false
            forceLoadedChunks.remove(chunk)
        }
    }
    
    private fun getChunkLoaderSet(chunk: ChunkPos) = forceLoadedChunks[chunk]
        ?: HashSet<UUID>().also { forceLoadedChunks[chunk] = it }
    
}