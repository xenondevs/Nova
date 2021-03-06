package xyz.xenondevs.nova.integration.protection.plugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.integration.InternalIntegration
import dev.espi.protectionstones.ProtectionStones as ProtectionStonesAPI

// ProtectionStones uses WorldGuard for regions, this integration is only preventing tile entities from breaking the protection stone.
internal object ProtectionStones : ProtectionIntegration, InternalIntegration {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("ProtectionStones") != null
    
    override fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location) =
        !ProtectionStonesAPI.isProtectBlock(location.block)
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location) = true
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location) = true
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location) = true
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location) = true
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) = true
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) = true
    
}