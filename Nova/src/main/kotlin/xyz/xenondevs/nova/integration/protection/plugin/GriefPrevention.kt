package xyz.xenondevs.nova.integration.protection.plugin

import me.ryanhamshire.GriefPrevention.GriefPrevention
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.integration.protection.FakeOnlinePlayer
import xyz.xenondevs.nova.integration.protection.ProtectionIntegration

object GriefPrevention : ProtectionIntegration {
    
    private val GRIEF_PREVENTION = if (Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) GriefPrevention.instance else null
    override val isInstalled = GRIEF_PREVENTION != null
    
    override fun canBreak(player: OfflinePlayer, location: Location) =
        GRIEF_PREVENTION?.allowBreak(FakeOnlinePlayer(player, location), location.block, location) == null
    
    override fun canPlace(player: OfflinePlayer, location: Location) =
        GRIEF_PREVENTION?.allowBuild(FakeOnlinePlayer(player, location), location) == null
    
    override fun canUse(player: OfflinePlayer, location: Location) = canBreak(player, location)
    
}