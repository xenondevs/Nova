package xyz.xenondevs.nova.hooks.impl.towny

import com.palmergames.bukkit.towny.`object`.TownyPermission
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.hooks.Hook
import xyz.xenondevs.nova.util.FakeOnlinePlayer

@Hook(plugins = ["Towny"])
internal object TownyHook : ProtectionIntegration {
    
    override val executionMode = ProtectionIntegration.ExecutionMode.NONE
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location) =
        hasPermission(player, location, TownyPermission.ActionType.DESTROY)
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location) =
        hasPermission(player, location, TownyPermission.ActionType.BUILD)
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location) =
        hasPermission(player, location, TownyPermission.ActionType.SWITCH)
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location) =
        hasPermission(player, location, TownyPermission.ActionType.ITEM_USE)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) =
        hasPermission(player, entity.location, TownyPermission.ActionType.ITEM_USE)
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) =
        hasPermission(player, entity.location, TownyPermission.ActionType.DESTROY)
    
    private fun hasPermission(player: OfflinePlayer, location: Location, actionType: TownyPermission.ActionType) =
        PlayerCacheUtil.getCachePermission(FakeOnlinePlayer(player, location), location, location.block.type, actionType)
    
}