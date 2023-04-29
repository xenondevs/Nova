package xyz.xenondevs.nova.hook.impl.protectionstones

import dev.espi.protectionstones.ProtectionStones
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.hook.Hook

// ProtectionStones uses WorldGuard for regions, this integration is only preventing tile entities from breaking the protection stone.
@Hook(plugins = ["ProtectionStones"])
internal object ProtectionStonesHook : ProtectionIntegration {
    
    override fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location) =
        !ProtectionStones.isProtectBlock(location.block)
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location) = true
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location) = true
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location) = true
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location) = true
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) = true
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) = true
    
}