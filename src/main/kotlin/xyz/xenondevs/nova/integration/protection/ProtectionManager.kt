package xyz.xenondevs.nova.integration.protection

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.integration.Integration
import xyz.xenondevs.nova.integration.protection.plugin.GriefPrevention
import xyz.xenondevs.nova.integration.protection.plugin.PlotSquared
import xyz.xenondevs.nova.integration.protection.plugin.WorldGuard
import xyz.xenondevs.nova.util.isBetweenXZ
import java.util.*

object ProtectionManager {
    
    val PROTECTION_PLUGINS = listOf(GriefPrevention, PlotSquared, WorldGuard).filter(Integration::isInstalled)
    
    fun canPlace(uuid: UUID, location: Location) =
        canPlace(Bukkit.getOfflinePlayer(uuid), location)
    
    fun canBreak(uuid: UUID, location: Location) =
        canBreak(Bukkit.getOfflinePlayer(uuid), location)
    
    fun canUse(uuid: UUID, location: Location) =
        canUse(Bukkit.getOfflinePlayer(uuid), location)
    
    fun canPlace(offlinePlayer: OfflinePlayer, location: Location) =
        !isVanillaProtected(offlinePlayer, location)
            && PROTECTION_PLUGINS.all { it.canPlace(offlinePlayer, location) }
    
    fun canBreak(offlinePlayer: OfflinePlayer, location: Location) =
        !isVanillaProtected(offlinePlayer, location)
            && PROTECTION_PLUGINS.all { it.canBreak(offlinePlayer, location) }
    
    fun canUse(offlinePlayer: OfflinePlayer, location: Location) =
        !isVanillaProtected(offlinePlayer, location)
            && PROTECTION_PLUGINS.all { it.canUse(offlinePlayer, location) }
    
    private fun isVanillaProtected(offlinePlayer: OfflinePlayer, location: Location): Boolean {
        val spawnRadius = Bukkit.getServer().spawnRadius.toDouble()
        val world = location.world!!
        return world.name == "world"
            && spawnRadius > 0
            && !offlinePlayer.isOp
            && location.isBetweenXZ(
            world.spawnLocation.subtract(spawnRadius, 0.0, spawnRadius),
            world.spawnLocation.add(spawnRadius, 0.0, spawnRadius)
        )
    }
    
}