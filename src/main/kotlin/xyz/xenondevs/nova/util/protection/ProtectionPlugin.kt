package xyz.xenondevs.nova.util.protection

import org.bukkit.Location
import org.bukkit.OfflinePlayer

abstract class ProtectionPlugin {
    
    abstract fun canBreak(player: OfflinePlayer, location: Location): Boolean
    
    abstract fun canPlace(player: OfflinePlayer, location: Location): Boolean
    
    abstract fun canUse(player: OfflinePlayer, location: Location): Boolean
    
}