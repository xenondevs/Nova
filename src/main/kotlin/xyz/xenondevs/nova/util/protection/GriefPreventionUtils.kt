package xyz.xenondevs.nova.util.protection

import me.ryanhamshire.GriefPrevention.GriefPrevention
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer

object GriefPreventionUtils {
    
    private val GRIEF_PREVENTION = if (Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) GriefPrevention.instance else null
    
    fun canBreak(player: OfflinePlayer, location: Location): Boolean {
        if (GRIEF_PREVENTION == null) return true
        return GRIEF_PREVENTION.allowBreak(FakeOnlinePlayer(player, location.world!!), location.block, location) == null
    }
    
    fun canPlace(player: OfflinePlayer, location: Location): Boolean {
        if (GRIEF_PREVENTION == null) return true
        return GRIEF_PREVENTION.allowBuild(FakeOnlinePlayer(player, location.world!!), location) == null
    }
    
}