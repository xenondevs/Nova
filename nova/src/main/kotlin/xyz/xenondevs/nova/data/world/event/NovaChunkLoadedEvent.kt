package xyz.xenondevs.nova.data.world.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.data.world.block.state.BlockState
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos

class NovaChunkLoadedEvent internal constructor(
    val chunkPos: ChunkPos,
    val blockStates: Map<BlockPos, BlockState>
) : Event() {
    
    companion object {
        @JvmStatic
        private val handlers = HandlerList()
        
        @JvmStatic
        fun getHandlerList() = handlers
    }
    
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
    
}