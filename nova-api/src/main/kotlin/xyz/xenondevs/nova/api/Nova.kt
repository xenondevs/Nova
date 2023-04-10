package xyz.xenondevs.nova.api

import org.bukkit.Bukkit
import xyz.xenondevs.nova.api.block.BlockManager
import xyz.xenondevs.nova.api.block.NovaBlockRegistry
import xyz.xenondevs.nova.api.item.NovaItemRegistry
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry
import xyz.xenondevs.nova.api.player.WailaManager
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.tileentity.TileEntityManager
import xyz.xenondevs.nova.loader.NovaLoader

@Suppress("DEPRECATION")
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
     * Used for breaking / placing blocks.
     */
    val blockManager: BlockManager
    
    /**
     * To look up nova materials
     */
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    val materialRegistry: NovaMaterialRegistry
    
    /**
     * To look up nova blocks
     */
    val blockRegistry: NovaBlockRegistry
    
    /**
     * To look up nova items
     */
    val itemRegistry: NovaItemRegistry
    
    /**
     * Manages the WAILA overlay
     */
    val wailaManager: WailaManager
    
    /**
     * Registers a [ProtectionIntegration]
     */
    fun registerProtectionIntegration(integration: ProtectionIntegration)
    
}
