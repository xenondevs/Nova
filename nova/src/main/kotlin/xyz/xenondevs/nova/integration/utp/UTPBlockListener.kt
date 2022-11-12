package xyz.xenondevs.nova.integration.utp

import io.github.unifiedtechpower.unified.core.UnifiedBlockListener
import io.github.unifiedtechpower.unified.core.UnifiedBlockManager
import io.github.unifiedtechpower.unified.energy.manager.UnifiedEnergy
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.UTPBlockState
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.pos
import java.util.*
import java.util.concurrent.CompletableFuture

internal object UTPBlockListener : UnifiedBlockListener, Listener {
    
    val chunkFutures: MutableMap<ChunkPos, CompletableFuture<Unit>> = Collections.synchronizedMap(HashMap())
    
    fun init() {
        UnifiedBlockManager.getInstance().registerBlockListener(this)
        registerEvents()
    }
    
    override fun handleBlockPlaced(location: Location) {
        val pos = location.pos
        WorldDataManager.runAfterChunkLoad(pos.chunkPos) {
            val currentState = WorldDataManager.getBlockState(pos)
            check(currentState == null) { "Block is already occupied by $currentState" }
            
            val blockState = UTPBlockState(pos)
            blockState.handleInitialized(true)
            WorldDataManager.setBlockState(pos, blockState)
        }
    }
    
    override fun handleBlockDestroyed(location: Location) {
        val pos = location.pos
        WorldDataManager.runAfterChunkLoad(pos.chunkPos) {
            val blockState = WorldDataManager.getBlockState(pos)
            if (blockState is UTPBlockState) {
                WorldDataManager.removeBlockState(pos)
                blockState.handleRemoved(true)
            }
        }
    }
    
    override fun handleChunkLoaded(chunk: Chunk) {
        val energyStorages = UnifiedEnergy.getInstance().getEnergyStoragesIn(chunk).get()
        
        WorldDataManager.runAfterChunkLoad(chunk.pos) {
            energyStorages.forEach { (loc, _) ->
                val pos = loc.pos
                if (WorldDataManager.getBlockState(pos) == null) {
                    val blockState = UTPBlockState(pos)
                    blockState.handleInitialized(true)
                    WorldDataManager.setBlockState(pos, blockState)
                }
            }
        }
    }
    
    @EventHandler
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.chunk
        val chunkPos = chunk.pos
        
        val future = chunkFutures.getOrPut(chunkPos, ::CompletableFuture)
        
        UnifiedEnergy.getInstance().getEnergyStoragesIn(event.chunk).thenAccept { energyStorages ->
            WorldDataManager.runAfterChunkLoad(chunkPos) {
                energyStorages.forEach { (loc, _) ->
                    val pos = loc.pos
                    if (WorldDataManager.getBlockState(pos) == null) {
                        val blockState = UTPBlockState(pos)
                        blockState.handleInitialized(true)
                        WorldDataManager.setBlockState(pos, blockState)
                    }
                }
                
                future.complete(Unit)
            }
        }
    }
    
    @EventHandler
    private fun handleChunkUnload(event: ChunkUnloadEvent) {
        // This removes already completed futures from the chunkFutures map to prevent the chunk directly being marked as
        // loaded when it is eventually loaded again
        chunkFutures -= event.chunk.pos
    }
    
}