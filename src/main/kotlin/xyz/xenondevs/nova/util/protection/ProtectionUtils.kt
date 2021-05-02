package xyz.xenondevs.nova.util.protection

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import java.util.*

object ProtectionUtils {
    
    fun canPlace(uuid: UUID, location: Location) =
        canPlace(Bukkit.getOfflinePlayer(uuid), location)
    
    fun canBreak(uuid: UUID, location: Location) =
        canBreak(Bukkit.getOfflinePlayer(uuid), location)
    
    fun canUse(uuid: UUID, location: Location) =
        canUse(Bukkit.getOfflinePlayer(uuid), location)
    
    fun canPlace(offlinePlayer: OfflinePlayer, location: Location) =
        WorldGuardUtils.canPlace(offlinePlayer, location)
            && GriefPreventionUtils.canPlace(offlinePlayer, location)
            && PlotSquaredUtils.isAllowed(offlinePlayer, location)
    
    fun canBreak(offlinePlayer: OfflinePlayer, location: Location) =
        WorldGuardUtils.canBreak(offlinePlayer, location)
            && GriefPreventionUtils.canBreak(offlinePlayer, location)
            && PlotSquaredUtils.isAllowed(offlinePlayer, location)
    
    fun canUse(offlinePlayer: OfflinePlayer, location: Location) =
        WorldGuardUtils.canUse(offlinePlayer, location)
            && GriefPreventionUtils.canBreak(offlinePlayer, location)
            && PlotSquaredUtils.isAllowed(offlinePlayer, location)
    
}