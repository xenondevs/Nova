package xyz.xenondevs.nova.integration.protection.plugin

import com.palmergames.bukkit.towny.`object`.TownyPermission.ActionType
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.protection.FakeOnlinePlayer
import xyz.xenondevs.nova.integration.protection.InternalProtectionIntegration

object Towny : InternalProtectionIntegration {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("Towny") != null
    override val canRunAsync = true
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location) =
        hasPermission(player, location, ActionType.DESTROY)
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location) =
        hasPermission(player, location, ActionType.BUILD)
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location) =
        hasPermission(player, location, ActionType.SWITCH)
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location) =
        hasPermission(player, location, ActionType.ITEM_USE)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) =
        hasPermission(player, entity.location, ActionType.ITEM_USE)
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) =
        hasPermission(player, entity.location, ActionType.DESTROY)
    
    private fun hasPermission(player: OfflinePlayer, location: Location, actionType: ActionType) =
        PlayerCacheUtil.getCachePermission(FakeOnlinePlayer(player, location), location, location.block.type, actionType)
    
}