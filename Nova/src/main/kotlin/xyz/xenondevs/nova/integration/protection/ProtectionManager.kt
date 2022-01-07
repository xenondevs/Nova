package xyz.xenondevs.nova.integration.protection

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.integration.Integration
import xyz.xenondevs.nova.integration.protection.plugin.*
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.isBetweenXZ

object ProtectionManager {
    
    private val PROTECTION_PLUGINS = listOf(GriefPrevention, PlotSquared, WorldGuard, GriefDefender, Towny, EventIntegration)
        .filter(Integration::isInstalled)
    
    fun canPlace(tileEntity: TileEntity, location: Location) =
        canPlace(tileEntity.owner, location)
    
    fun canPlace(offlinePlayer: OfflinePlayer, location: Location) =
        !isVanillaProtected(offlinePlayer, location)
            && PROTECTION_PLUGINS.all { it.canPlace(offlinePlayer, location) }

    fun canBreak(tileEntity: TileEntity, location: Location) =
        canBreak(tileEntity.owner, location)
    
    fun canBreak(offlinePlayer: OfflinePlayer, location: Location) =
        !isVanillaProtected(offlinePlayer, location)
            && PROTECTION_PLUGINS.all { it.canBreak(offlinePlayer, location) }

    fun canUse(tileEntity: TileEntity, location: Location) =
        canUse(tileEntity.owner, location)
    
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