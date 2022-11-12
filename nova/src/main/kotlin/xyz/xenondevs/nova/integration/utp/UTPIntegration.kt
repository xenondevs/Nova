package xyz.xenondevs.nova.integration.utp

import io.github.unifiedtechpower.unified.core.UnifiedBlockManager
import org.bukkit.Bukkit
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.integration.utp.energy.UTPEnergyNetworkManager
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.ChunkPos
import java.util.concurrent.CompletableFuture

internal object UTPIntegration : Initializable() {
    
    override val initializationStage = InitializationStage.POST_WORLD
    override val dependsOn = setOf(NetworkManager)
    
    private val INSTALLED = Bukkit.getPluginManager().getPlugin("Unified") != null
    
    override fun init() {
        if (INSTALLED) {
            UTPBlockListener.init()
            UTPEnergyNetworkManager.init()
        }
    }
    
    fun handleTileEntityPlace(tileEntity: TileEntity) {
        if (!INSTALLED)
            return
    
        if (tileEntity is NetworkedTileEntity) {
            UnifiedBlockManager.getInstance().callBlockPlaced(tileEntity.location, UTPBlockListener)
        }
    }
    
    fun handleTileEntityBreak(tileEntity: TileEntity) {
        if (!INSTALLED)
            return
        
        if (tileEntity is NetworkedTileEntity) {
            UnifiedBlockManager.getInstance().callBlockDestroyed(tileEntity.location, UTPBlockListener)
        }
    }
    
    fun isChunkLoaded(pos: ChunkPos): Boolean {
        if (INSTALLED) {
            return UTPBlockListener.chunkFutures.getOrPut(pos, ::CompletableFuture).isDone
        }
        
        return true
    }
    
}