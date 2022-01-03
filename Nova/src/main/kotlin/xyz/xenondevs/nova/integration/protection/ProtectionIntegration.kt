package xyz.xenondevs.nova.integration.protection

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.integration.Integration

interface ProtectionIntegration : Integration {
    
    fun canBreak(player: OfflinePlayer, location: Location): Boolean
    
    fun canPlace(player: OfflinePlayer, location: Location): Boolean
    
    fun canUse(player: OfflinePlayer, location: Location): Boolean
    
}