package xyz.xenondevs.nova.world.block.tileentity

import org.bukkit.Bukkit
import xyz.xenondevs.commons.collections.removeIf
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.world.ChunkPos
import java.util.*

@InternalInit(stage = InternalInitStage.POST_WORLD)
object ChunkLoadManager {
    
    private val forceLoadedChunks = PermanentStorage.retrieve("forceLoadedChunks") { HashMap<ChunkPos, HashSet<UUID>>() }
    
    @InitFun
    private fun init() {
        Bukkit.getWorlds().flatMap { it.forceLoadedChunks }.forEach { it.isForceLoaded = false }
        forceLoadedChunks.removeIf { it.key.chunk?.apply { isForceLoaded = true } == null }
    }
    
    @DisableFun
    private fun disable() {
        PermanentStorage.store("forceLoadedChunks", forceLoadedChunks)
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