package xyz.xenondevs.nova.api

import org.bukkit.Bukkit
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.tileentity.TileEntityManager

interface Nova {
    
    companion object : Nova by (Bukkit.getPluginManager().getPlugin("Nova") as Nova) {
        
        @JvmStatic
        fun getNova(): Nova = this
        
    }
    
    /**
     * Used for managing tile-entities
     */
    val tileEntityManager: TileEntityManager
    
    /**
     * To look up nova materials
     */
    val materialRegistry: NovaMaterialRegistry
    
    /**
     * Registers a [ProtectionIntegration]
     */
    fun registerProtectionIntegration(integration: ProtectionIntegration)
    
}
