package xyz.xenondevs.nova.tileentity

import org.bukkit.Chunk
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.config.PermanentStorage
import java.util.*

object ChunkLoadManager {
    
    private val forceLoadedChunks = PermanentStorage.retrieve("forceLoadedChunks") { HashMap<Chunk, HashSet<UUID>>() }
    
    fun init() {
        NOVA.disableHandlers.add { PermanentStorage.store("forceLoadedChunks", forceLoadedChunks) }
        forceLoadedChunks.keys.forEach { it.world.setChunkForceLoaded(it.x, it.z, true) }
    }
    
    fun submitChunkLoadRequest(chunk: Chunk, uuid: UUID) {
        val requesterSet = getChunkLoaderSet(chunk)
        if (requesterSet.isEmpty()) chunk.world.setChunkForceLoaded(chunk.x, chunk.z, true)
        requesterSet.add(uuid)
    }
    
    fun revokeChunkLoadRequest(chunk: Chunk, uuid: UUID) {
        val requesterSet = getChunkLoaderSet(chunk)
        requesterSet.remove(uuid)
        if (requesterSet.isEmpty()) {
            chunk.world.setChunkForceLoaded(chunk.x, chunk.z, false)
            forceLoadedChunks.remove(chunk)
        }
    }
    
    private fun getChunkLoaderSet(chunk: Chunk) = forceLoadedChunks[chunk]
        ?: HashSet<UUID>().also { forceLoadedChunks[chunk] = it }
    
}