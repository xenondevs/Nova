package xyz.xenondevs.nova.integration.protection.plugin

import me.ryanhamshire.GriefPrevention.GriefPrevention
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.protection.FakeOnlinePlayer
import xyz.xenondevs.nova.integration.protection.InternalProtectionIntegration

object GriefPrevention : InternalProtectionIntegration {
    
    private val GRIEF_PREVENTION = if (Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) GriefPrevention.instance else null
    override val isInstalled = GRIEF_PREVENTION != null
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location) =
        GRIEF_PREVENTION?.allowBreak(FakeOnlinePlayer(player, location), location.block, location) == null
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location) =
        GRIEF_PREVENTION?.allowBuild(FakeOnlinePlayer(player, location), location) == null
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location) =
        canBreak(player, item, location)
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location) =
        canBreak(player, item, location)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) =
        canBreak(player, item, entity.location)
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) =
        canBreak(player, item, entity.location)
    
}