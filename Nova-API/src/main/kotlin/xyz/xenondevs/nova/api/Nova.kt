package xyz.xenondevs.nova.api

import org.bukkit.Bukkit
import xyz.xenondevs.nova.api.protection.ProtectionIntegration

interface Nova {
    
    companion object : Nova by (Bukkit.getPluginManager().getPlugin("Nova") as Nova) {
        
        @JvmStatic
        fun getNova(): Nova = this
        
    }
    
    /**
     * Registers a [ProtectionIntegration]
     */
    fun registerProtectionIntegration(integration: ProtectionIntegration)
    
}
