package xyz.xenondevs.nova.world.block.tileentity

import org.bukkit.Bukkit
import xyz.xenondevs.nova.Nova
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
        Bukkit.getWorlds().flatMap { it.forceLoadedChunks }.forEach { it.isForceLoaded = false } // TODO: remove in future version
        
        for (chunk in forceLoadedChunks.keys) {
            chunk.world?.addPluginChunkTicket(chunk.x, chunk.z, Nova)
        }
    }
    
    @DisableFun
    private fun disable() {
        PermanentStorage.store("forceLoadedChunks", forceLoadedChunks)
    }
    
    fun submitChunkLoadRequest(chunk: ChunkPos, uuid: UUID) {
        val requesterSet = getChunkLoaderSet(chunk)
        if (requesterSet.isEmpty()) {
            chunk.world?.addPluginChunkTicket(chunk.x, chunk.z, Nova)
        }
        requesterSet.add(uuid)
    }
    
    fun revokeChunkLoadRequest(chunk: ChunkPos, uuid: UUID) {
        val requesterSet = getChunkLoaderSet(chunk)
        requesterSet.remove(uuid)
        if (requesterSet.isEmpty()) {
            chunk.world?.removePluginChunkTicket(chunk.x, chunk.z, Nova)
            forceLoadedChunks.remove(chunk)
        }
    }
    
    private fun getChunkLoaderSet(chunk: ChunkPos) = forceLoadedChunks[chunk]
        ?: HashSet<UUID>().also { forceLoadedChunks[chunk] = it }
    
}