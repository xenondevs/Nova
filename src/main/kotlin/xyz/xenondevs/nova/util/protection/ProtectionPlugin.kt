package xyz.xenondevs.nova.util.protection

import org.bukkit.Location
import org.bukkit.OfflinePlayer

interface ProtectionPlugin {
    
    fun canBreak(player: OfflinePlayer, location: Location): Boolean
    
    fun canPlace(player: OfflinePlayer, location: Location): Boolean
    
    fun canUse(player: OfflinePlayer, location: Location): Boolean
    
}