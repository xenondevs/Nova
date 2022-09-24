package xyz.xenondevs.nova.api

import org.bukkit.Bukkit
import xyz.xenondevs.nova.api.block.BlockManager
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry
import xyz.xenondevs.nova.api.player.WailaManager
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.tileentity.TileEntityManager
import xyz.xenondevs.nova.loader.NovaLoader

interface Nova {
    
    companion object : Nova by (Bukkit.getPluginManager().getPlugin("Nova") as NovaLoader).nova as Nova {
        
        @JvmStatic
        fun getNova(): Nova = this
        
    }
    
    /**
     * Used for managing tile-entities
     */
    val tileEntityManager: TileEntityManager
    
    /**
     * Used for managing blocks
     */
    val blockManager: BlockManager
    
    /**
     * To look up nova materials
     */
    val materialRegistry: NovaMaterialRegistry
    
    /**
     * Manages the WAILA overlay
     */
    val wailaManager: WailaManager
    
    /**
     * Registers a [ProtectionIntegration]
     */
    fun registerProtectionIntegration(integration: ProtectionIntegration)
    
}
