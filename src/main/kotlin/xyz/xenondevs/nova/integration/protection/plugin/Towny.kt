package xyz.xenondevs.nova.integration.protection.plugin

import com.palmergames.bukkit.towny.`object`.TownyPermission.ActionType
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.integration.protection.FakeOnlinePlayer
import xyz.xenondevs.nova.integration.protection.ProtectionIntegration

object Towny : ProtectionIntegration {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("Towny") != null
    
    override fun canBreak(player: OfflinePlayer, location: Location) =
        hasPermission(player, location, ActionType.DESTROY)
    
    override fun canPlace(player: OfflinePlayer, location: Location) =
        hasPermission(player, location, ActionType.BUILD)
    
    override fun canUse(player: OfflinePlayer, location: Location) =
        hasPermission(player, location, ActionType.ITEM_USE)
    
    private fun hasPermission(player: OfflinePlayer, location: Location, actionType: ActionType) =
        PlayerCacheUtil.getCachePermission(FakeOnlinePlayer(player, location), location, location.block.type, actionType)
    
}